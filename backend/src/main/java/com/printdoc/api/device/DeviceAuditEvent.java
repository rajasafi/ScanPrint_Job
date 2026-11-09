package com.printdoc.api.device;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="device_audit_events",indexes=@Index(name="idx_device_audit_device_time",columnList="device_id,created_at"))
public class DeviceAuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="device_id",nullable=false) private AgentDevice device;
    @Column(nullable=false,length=32) private String event;
    @Column(nullable=false,length=80) private String actor;
    @Column(length=300) private String note;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected DeviceAuditEvent() {}
    public DeviceAuditEvent(AgentDevice device,String event,String actor,String note){this.device=device;this.event=event;this.actor=actor;this.note=note;}
}
