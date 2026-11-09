package com.printdoc.api.device;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DeviceAuditEventRepository extends JpaRepository<DeviceAuditEvent,UUID> {}
