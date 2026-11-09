package com.printdoc.api.device;

import com.printdoc.api.common.ApiException;
import com.printdoc.api.shop.Shop;
import com.printdoc.api.shop.ShopRepository;
import com.printdoc.api.shop.ShopUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DeviceService {
    private static final long OFFLINE_AFTER_SECONDS=90;
    private final ShopRepository shops;
    private final AgentDeviceRepository devices;
    private final DeviceAuditEventRepository audit;
    private final PasswordEncoder passwords;
    private final SecureRandom random=new SecureRandom();

    public DeviceService(ShopRepository shops,AgentDeviceRepository devices,DeviceAuditEventRepository audit,PasswordEncoder passwords){
        this.shops=shops;this.devices=devices;this.audit=audit;this.passwords=passwords;
    }

    @Transactional
    public DeviceDtos.CredentialView issueCredentials(ShopUser owner){
        Shop shop=shops.findById(requireOwnerShop(owner).getId()).orElseThrow(()->ApiException.notFound("Shop was not found."));
        String secret=Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));
        Instant now=Instant.now();
        shop.replaceAgentSecret(passwords.encode(secret),now);
        return new DeviceDtos.CredentialView(shop.getShopCode(),secret,now);
    }

    @Transactional
    public DeviceDtos.StatusView checkStatus(String shopCode,String fingerprint){
        Shop shop=requireActiveShop(shopCode);
        String normalized=normalizeFingerprint(fingerprint);
        AgentDevice device=devices.findByShopIdAndFingerprint(shop.getId(),normalized).orElseGet(()->{
            if(devices.countByShopIdAndStatus(shop.getId(),AgentDevice.Status.PENDING)>=20)
                throw ApiException.conflict("This shop has reached the pending device approval limit.");
            AgentDevice created=devices.save(new AgentDevice(shop,normalized));
            audit.save(new DeviceAuditEvent(created,"DEVICE_REQUESTED","AGENT","New device requested approval."));
            return created;
        });
        return statusView(device);
    }

    @Transactional(readOnly=true)
    public DeviceDtos.DeviceList ownerDevices(ShopUser owner){
        Shop shop=requireOwnerShop(owner);
        List<DeviceDtos.DeviceView> views=devices.findAllByShopIdOrderByCreatedAtDesc(shop.getId()).stream().map(this::deviceView).toList();
        return new DeviceDtos.DeviceList(views);
    }

    @Transactional
    public DeviceDtos.DeviceView approve(ShopUser owner,UUID deviceId){return change(owner,deviceId,"APPROVE");}
    @Transactional
    public DeviceDtos.DeviceView reject(ShopUser owner,UUID deviceId){return change(owner,deviceId,"REJECT");}
    @Transactional
    public DeviceDtos.DeviceView revoke(ShopUser owner,UUID deviceId){return change(owner,deviceId,"REVOKE");}

    @Transactional
    public DeviceDtos.DeviceView register(DeviceDtos.AgentRegisterRequest request,String secretApiKey){
        AgentDevice device=authenticateAgent(request.shopCode(),secretApiKey,request.deviceFingerprint());
        Instant now=Instant.now();
        device.updateRegistration(request.agentVersion(),request.hostname(),request.platform(),request.osPlatform(),request.ipAddress(),request.deviceMeta(),now);
        audit.save(new DeviceAuditEvent(device,"AGENT_REGISTERED","AGENT","Agent authenticated and registered."));
        return deviceView(device);
    }

    @Transactional
    public DeviceDtos.DeviceView heartbeat(DeviceDtos.AgentHeartbeatRequest request,String secretApiKey){
        AgentDevice device=authenticateAgent(request.shopCode(),secretApiKey,request.deviceFingerprint());
        return touchHeartbeat(device);
    }

    /** Updates a connection that was already authenticated during the Socket.IO handshake. */
    @Transactional
    public DeviceDtos.DeviceView socketHeartbeat(String shopCode,String fingerprint){
        AgentDevice device=approvedDevice(shopCode,fingerprint);
        return touchHeartbeat(device);
    }

    /** Stores registration metadata after the Socket.IO handshake has authenticated the device. */
    @Transactional
    public DeviceDtos.DeviceView socketRegister(String shopCode,String fingerprint,String agentVersion,String hostname,String platform,String osPlatform,String ipAddress,String deviceMeta){
        AgentDevice device=approvedDevice(shopCode,fingerprint);
        device.updateRegistration(agentVersion,hostname,platform,osPlatform,ipAddress,deviceMeta,Instant.now());
        audit.save(new DeviceAuditEvent(device,"AGENT_REGISTERED","AGENT","Socket.IO agent registered."));
        return deviceView(device);
    }

    @Transactional(readOnly=true)
    public AgentDevice authenticateAgent(String shopCode,String secretApiKey,String fingerprint){
        Shop shop=requireActiveShop(shopCode);
        if(secretApiKey==null||secretApiKey.isBlank()||shop.getAgentSecretHash()==null||!passwords.matches(secretApiKey,shop.getAgentSecretHash()))
            throw ApiException.unauthorized("The agent credentials are invalid.");
        String normalized=normalizeFingerprint(fingerprint);
        AgentDevice device=devices.findByShopIdAndFingerprint(shop.getId(),normalized).orElseThrow(()->ApiException.forbidden("This device has not requested approval."));
        if(device.getStatus()!=AgentDevice.Status.APPROVED)throw ApiException.forbidden("This device is not approved for agent access.");
        return device;
    }

    private AgentDevice approvedDevice(String shopCode,String fingerprint){
        Shop shop=requireActiveShop(shopCode);
        AgentDevice device=devices.findByShopIdAndFingerprint(shop.getId(),normalizeFingerprint(fingerprint)).orElseThrow(()->ApiException.forbidden("This device has not requested approval."));
        if(device.getStatus()!=AgentDevice.Status.APPROVED)throw ApiException.forbidden("This device is not approved for agent access.");
        return device;
    }

    private DeviceDtos.DeviceView touchHeartbeat(AgentDevice device){
        Instant now=Instant.now();
        device.updateRegistration(device.getAgentVersion(),device.getHostname(),device.getPlatform(),device.getOsPlatform(),device.getIpAddress(),device.getDeviceMeta(),now);
        return deviceView(device);
    }

    private DeviceDtos.DeviceView change(ShopUser owner,UUID deviceId,String action){
        Shop shop=requireOwnerShop(owner);
        AgentDevice device=devices.findById(deviceId).filter(d->d.getShop().getId().equals(shop.getId())).orElseThrow(()->ApiException.notFound("Device was not found."));
        Instant now=Instant.now();
        switch(action){
            case "APPROVE" -> {device.approve(now);audit.save(new DeviceAuditEvent(device,"DEVICE_APPROVED",owner.getEmail(),"Owner approved the device."));}
            case "REJECT" -> {device.reject();audit.save(new DeviceAuditEvent(device,"DEVICE_REJECTED",owner.getEmail(),"Owner rejected the device."));}
            case "REVOKE" -> {device.revoke(now);audit.save(new DeviceAuditEvent(device,"DEVICE_REVOKED",owner.getEmail(),"Owner revoked the device."));}
            default -> throw new IllegalArgumentException("Unknown device action");
        }
        return deviceView(device);
    }

    private Shop requireOwnerShop(ShopUser owner){
        if(owner==null||owner.getShop()==null||(!owner.getRole().equals(ShopUser.Role.OWNER)&&!owner.getRole().equals(ShopUser.Role.ADMIN)))throw ApiException.forbidden("Shop owner access is required.");
        return owner.getShop();
    }
    private Shop requireActiveShop(String shopCode){
        if(shopCode==null||shopCode.isBlank())throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,"Shop code is required.");
        return shops.findByShopCodeIgnoreCase(shopCode.trim()).filter(Shop::isActive).orElseThrow(()->ApiException.notFound("Shop was not found."));
    }
    private String normalizeFingerprint(String fingerprint){
        if(fingerprint==null||fingerprint.isBlank()||fingerprint.length()>128)throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,"A valid device fingerprint is required.");
        String value=fingerprint.trim();
        if(value.chars().anyMatch(Character::isISOControl))throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,"A valid device fingerprint is required.");
        return value;
    }
    private DeviceDtos.StatusView statusView(AgentDevice device){
        Instant now=Instant.now();
        return new DeviceDtos.StatusView(device.getId(),device.getShop().getShopCode(),device.getFingerprint(),device.getStatus(),device.getStatus()==AgentDevice.Status.APPROVED,device.isOnline(now,OFFLINE_AFTER_SECONDS),device.getLastSeenAt());
    }
    private DeviceDtos.DeviceView deviceView(AgentDevice device){
        Instant now=Instant.now();
        return new DeviceDtos.DeviceView(device.getId(),device.getShop().getShopCode(),device.getFingerprint(),device.getStatus(),device.isOnline(now,OFFLINE_AFTER_SECONDS),device.getAgentVersion(),device.getHostname(),device.getPlatform(),device.getIpAddress(),device.getLastSeenAt(),device.getCreatedAt());
    }
    private byte[] randomBytes(int length){byte[] bytes=new byte[length];random.nextBytes(bytes);return bytes;}
}
