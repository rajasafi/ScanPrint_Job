package com.printdoc.api.device;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentDeviceRepository extends JpaRepository<AgentDevice,UUID> {
    Optional<AgentDevice> findByShopIdAndFingerprint(UUID shopId,String fingerprint);
    List<AgentDevice> findAllByShopIdOrderByCreatedAtDesc(UUID shopId);
    long countByShopIdAndStatus(UUID shopId,AgentDevice.Status status);
}
