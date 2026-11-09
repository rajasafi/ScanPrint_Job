package com.printdoc.api.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DeviceDtos {
    private DeviceDtos() {}
    public record CheckStatus(String shopCode,String fingerprint) {}
    public record StatusView(UUID deviceId,String shopCode,String fingerprint,AgentDevice.Status status,boolean approved,boolean online,Instant lastSeenAt) {}
    public record DeviceView(UUID deviceId,String shopCode,String fingerprint,AgentDevice.Status status,boolean online,String agentVersion,String hostname,String platform,String ipAddress,Instant lastSeenAt,Instant createdAt) {}
    public record CredentialView(String shopCode,String secretApiKey,Instant issuedAt) {}
    public record AgentRegisterRequest(@NotBlank @Size(max=32) String shopCode,@NotBlank @Size(max=128) String deviceFingerprint,@Size(max=40) String agentVersion,@Size(max=120) String hostname,@Size(max=40) String platform,@Size(max=180) String osPlatform,@Size(max=64) String ipAddress,@Size(max=4000) String deviceMeta) {}
    public record AgentHeartbeatRequest(@NotBlank @Size(max=32) String shopCode,@NotBlank @Size(max=128) String deviceFingerprint) {}
    public record DeviceList(List<DeviceView> devices) {}
}
