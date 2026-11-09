package com.printdoc.api.socket;

import com.corundumstudio.socketio.AuthTokenResult;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.printdoc.api.common.ApiException;
import com.printdoc.api.device.DeviceService;
import com.printdoc.api.device.DeviceDtos;
import com.printdoc.api.job.JobDtos;
import com.printdoc.api.job.JobService;
import com.printdoc.api.job.PrintJob;
import com.printdoc.api.shop.ShopDtos;
import com.printdoc.api.shop.ShopService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Socket.IO v4 gateway compatible with the reference desktop agent.
 * It intentionally runs as a separately configured Netty listener; place a
 * reverse proxy in front of this listener and the REST API for one public URL.
 */
@Component
@ConditionalOnProperty(name="printdoc.socket.enabled",havingValue="true")
public class SocketAgentGateway implements AgentSocketPublisher {
    private static final Logger log=LoggerFactory.getLogger(SocketAgentGateway.class);
    private static final String ROOM_PREFIX="shop:";
    private final DeviceService devices;
    private final JobService jobs;
    private final ShopService shops;
    private final ObjectMapper mapper;
    private final String hostname;
    private final int port;
    private final String context;
    private final String allowedOrigin;
    private SocketIOServer server;

    public SocketAgentGateway(DeviceService devices,JobService jobs,ShopService shops,
                              @Value("${printdoc.socket.hostname:0.0.0.0}") String hostname,
                              @Value("${printdoc.socket.port:9092}") int port,
                              @Value("${printdoc.socket.context:/socket.io}") String context,
                              @Value("${printdoc.socket.allowed-origin:*}") String allowedOrigin){
        this.devices=devices;this.jobs=jobs;this.shops=shops;this.mapper=new ObjectMapper();this.hostname=hostname;this.port=port;this.context=context;this.allowedOrigin=allowedOrigin;
    }

    @PostConstruct
    public void start(){
        Configuration config=new Configuration();
        config.setHostname(hostname);config.setPort(port);config.setContext(context);config.setOrigin(allowedOrigin);config.setPingInterval(25_000);config.setPingTimeout(60_000);config.setMaxHttpContentLength(128*1024);config.setMaxFramePayloadLength(128*1024);
        server=new SocketIOServer(config);
        SocketIONamespace namespace=server.getNamespace("");
        namespace.addAuthTokenListener(this::authorizeAuthToken);
        server.addConnectListener(this::connected);
        server.addDisconnectListener(this::disconnected);
        server.addEventListener("AGENT_REGISTER",Map.class,(client,data,ack)->register(client,data));
        server.addEventListener("AGENT_HEARTBEAT",Map.class,(client,data,ack)->heartbeat(client,data));
        server.addEventListener("JOB_SUCCESS",Map.class,(client,data,ack)->jobResult(client,data,PrintJob.Status.COMPLETED));
        server.addEventListener("JOB_FAILED",Map.class,(client,data,ack)->jobResult(client,data,PrintJob.Status.FAILED));
        server.addEventListener("AGENT_PRINTERS_UPDATED",Map.class,(client,data,ack)->printerUpdate(client,data));
        server.addEventListener("AGENT_CONFIG_UPDATE_ACK",Map.class,(client,data,ack)->log.debug("Agent config acknowledgement from {}: {}",client.getSessionId(),data));
        server.start();
        log.info("Socket.IO agent gateway listening on {}:{}{}",hostname,port,context);
    }

    @PreDestroy
    public void stop(){if(server!=null){server.stop();log.info("Socket.IO agent gateway stopped");}}

    private AuthTokenResult authorizeAuthToken(Object authToken,SocketIOClient client){
        try{
            Map<?,?> auth=asMap(authToken);
            String shopCode=first(auth,"shopId","shopCode");
            String secret=first(auth,"secretKey","secretApiKey");
            String fingerprint=first(auth,"deviceFingerprint");
            String agentType=first(auth,"agentType");
            if(!"DESKTOP_WIN_AGENT".equals(agentType))return denied("Unsupported agent type.");
            var device=devices.authenticateAgent(shopCode,secret,fingerprint);
            client.set("shopCode",device.getShop().getShopCode());
            client.set("shopUuid",device.getShop().getId().toString());
            client.set("fingerprint",device.getFingerprint());
            client.set("deviceId",device.getId().toString());
            return AuthTokenResult.AuthTokenResultSuccess;
        }catch(Exception ex){
            log.warn("Rejected Socket.IO agent handshake from {}: {}",remote(client),safeMessage(ex));
            return denied("Agent authorization failed.");
        }
    }

    private void connected(SocketIOClient client){
        String shopCode=client.get("shopCode");
        if(shopCode==null){client.disconnect();return;}
        client.joinRoom(room(shopCode));
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("shopCode",shopCode);payload.put("deviceId",client.<String>get("deviceId"));payload.put("message","Agent authenticated.");
        client.sendEvent("AGENT_CONNECTED",payload);
        log.info("Agent connected: shop={} device={} remote={}",shopCode,client.<String>get("deviceId"),remote(client));
    }

    private void disconnected(SocketIOClient client){log.info("Agent disconnected: shop={} device={} remote={}",client.<String>get("shopCode"),client.<String>get("deviceId"),remote(client));}

    private void register(SocketIOClient client,Map<?,?> data){
        try{
            String shopCode=session(client,"shopCode");String fingerprint=session(client,"fingerprint");
            DeviceDtos.DeviceView view=devices.socketRegister(shopCode,fingerprint,first(data,"agentVersion"),first(data,"hostname"),first(data,"platform"),first(data,"osPlatform"),first(data,"ipAddress","localIp","publicIp"),json(data.get("deviceMeta")));
            ShopDtos.PublicShop shop=shops.publicByCode(shopCode);
            Map<String,Object> payload=new LinkedHashMap<>();payload.put("shopCode",shop.shopCode());payload.put("shopName",shop.displayName());payload.put("deviceId",view.deviceId());payload.put("agentVersion",view.agentVersion());payload.put("bwRate",shop.bwRateMinor());payload.put("colorRate",shop.colorRateMinor());payload.put("duplexRate",shop.duplexRateMinor());
            client.sendEvent("AGENT_CONNECTED",payload);log.info("Agent registration accepted: shop={} device={}",shopCode,view.deviceId());
        }catch(Exception ex){sendError(client,"Agent registration failed.",ex);}
    }

    private void heartbeat(SocketIOClient client,Map<?,?> data){
        try{String shopCode=session(client,"shopCode");String fingerprint=session(client,"fingerprint");devices.socketHeartbeat(shopCode,fingerprint);}
        catch(Exception ex){sendError(client,"Agent heartbeat rejected.",ex);}
    }

    private void jobResult(SocketIOClient client,Map<?,?> data,PrintJob.Status next){
        try{
            String jobId=first(data,"jobId");if(jobId==null||jobId.isBlank())throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,"Job ID is required.");
            JobDtos.JobView result=jobs.transitionFromAgent(jobId,shopId(client),next,next==PrintJob.Status.COMPLETED?"Agent reported successful printing.":first(data,"error","errorMessage"));
            publishJobStatus(session(client,"shopCode"),result);
        }catch(Exception ex){sendError(client,"Job result rejected.",ex);}
    }

    private void printerUpdate(SocketIOClient client,Map<?,?> data){log.info("Agent printer inventory received: shop={} device={} payloadKeys={}",client.<String>get("shopCode"),client.<String>get("deviceId"),data==null?0:data.size());}

    @Override public void publishJobStatus(String shopCode,JobDtos.JobView job){
        if(server==null)return;Map<String,Object> payload=new LinkedHashMap<>();payload.put("jobId",job.jobId());payload.put("status",job.status());payload.put("job",job);server.getNamespace("").getRoomOperations(room(shopCode)).sendEvent("JOB_STATUS_UPDATED",payload);
    }
    @Override public void dispatch(String shopCode,Object jobPayload){
        if(server==null)return;
        Object payload=jobPayload;
        if(jobPayload instanceof JobDtos.JobView queued){
            payload=jobs.startPrinting(queued.jobId(),shopCode);
            publishJobStatus(shopCode,(JobDtos.JobView)payload);
        }
        server.getNamespace("").getRoomOperations(room(shopCode)).sendEvent("PRINT_JOB_DISPATCH",payload);
    }
    @Override public void cancel(String shopCode,String jobId){if(server!=null)server.getNamespace("").getRoomOperations(room(shopCode)).sendEvent("CANCEL_HELD_JOB",Map.of("jobId",jobId));}
    @Override public void requestPrinterScan(String shopCode){if(server!=null)server.getNamespace("").getRoomOperations(room(shopCode)).sendEvent("REQUEST_PRINTER_SCAN",Map.of("shopCode",shopCode));}

    private UUID shopId(SocketIOClient client){return UUID.fromString(session(client,"shopUuid"));}
    private String session(SocketIOClient client,String key){String value=client.get(key);if(value==null||value.isBlank())throw ApiException.unauthorized("Agent session is not authenticated.");return value;}
    private static String room(String shopCode){return ROOM_PREFIX+shopCode.toUpperCase(java.util.Locale.ROOT);}
    private static AuthTokenResult denied(String message){return new AuthTokenResult(false,message);}
    private Map<?,?> asMap(Object value){if(value instanceof Map<?,?> map)return map;return mapper.convertValue(value,Map.class);}
    private String first(Map<?,?> map,String... keys){if(map==null)return null;for(String key:keys){Object value=map.get(key);if(value!=null&&!String.valueOf(value).isBlank())return String.valueOf(value);}return null;}
    private String json(Object value){if(value==null)return null;if(value instanceof String s)return s;try{return mapper.writeValueAsString(value);}catch(JsonProcessingException e){return null;}}
    private void sendError(SocketIOClient client,String message,Exception ex){log.warn("{} shop={} device={}: {}",message,client.<String>get("shopCode"),client.<String>get("deviceId"),safeMessage(ex));client.sendEvent("AGENT_AUTH_ERROR",Map.of("message",message));}
    private static String safeMessage(Exception ex){return ex instanceof ApiException?ex.getMessage():ex.getClass().getSimpleName();}
    private static String remote(SocketIOClient client){if(client.getRemoteAddress() instanceof InetSocketAddress address)return String.valueOf(address.getAddress());return String.valueOf(client.getRemoteAddress());}
}
