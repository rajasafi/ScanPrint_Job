package com.printdoc.api.device;

import com.printdoc.api.shop.Shop;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="agent_devices",
    uniqueConstraints=@UniqueConstraint(name="uk_device_shop_fingerprint",columnNames={"shop_id","fingerprint"}),
    indexes={@Index(name="idx_device_shop_status",columnList="shop_id,status"),@Index(name="idx_device_last_seen",columnList="last_seen_at")})
public class AgentDevice {
    public enum Status { PENDING, APPROVED, REJECTED, REVOKED }
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @Column(nullable=false,length=128) private String fingerprint;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Status status=Status.PENDING;
    @Column(name="agent_version",length=40) private String agentVersion;
    @Column(length=120) private String hostname;
    @Column(length=40) private String platform;
    @Column(name="os_platform",length=180) private String osPlatform;
    @Column(name="ip_address",length=64) private String ipAddress;
    @Column(name="device_meta",length=4000) private String deviceMeta;
    @Column(name="last_seen_at") private Instant lastSeenAt;
    @Column(name="approved_at") private Instant approvedAt;
    @Column(name="revoked_at") private Instant revokedAt;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected AgentDevice() {}
    public AgentDevice(Shop shop,String fingerprint){this.shop=shop;this.fingerprint=fingerprint;}
    public UUID getId(){return id;} public Shop getShop(){return shop;} public String getFingerprint(){return fingerprint;} public Status getStatus(){return status;}
    public String getAgentVersion(){return agentVersion;} public String getHostname(){return hostname;} public String getPlatform(){return platform;}
    public String getOsPlatform(){return osPlatform;} public String getIpAddress(){return ipAddress;} public String getDeviceMeta(){return deviceMeta;}
    public Instant getLastSeenAt(){return lastSeenAt;} public Instant getApprovedAt(){return approvedAt;} public Instant getRevokedAt(){return revokedAt;} public Instant getCreatedAt(){return createdAt;}
    public void updateRegistration(String agentVersion,String hostname,String platform,String osPlatform,String ipAddress,String deviceMeta,Instant now){
        this.agentVersion=trim(agentVersion,40); this.hostname=trim(hostname,120); this.platform=trim(platform,40); this.osPlatform=trim(osPlatform,180); this.ipAddress=trim(ipAddress,64); this.deviceMeta=trim(deviceMeta,4000); this.lastSeenAt=now;
    }
    public void approve(Instant now){this.status=Status.APPROVED;this.approvedAt=now;this.revokedAt=null;}
    public void reject(){this.status=Status.REJECTED;}
    public void revoke(Instant now){this.status=Status.REVOKED;this.revokedAt=now;}
    public boolean isOnline(Instant now,long offlineAfterSeconds){return status==Status.APPROVED&&lastSeenAt!=null&&!lastSeenAt.isBefore(now.minusSeconds(offlineAfterSeconds));}
    private static String trim(String value,int max){if(value==null)return null;String v=value.trim();return v.length()<=max?v:v.substring(0,max);}
}
