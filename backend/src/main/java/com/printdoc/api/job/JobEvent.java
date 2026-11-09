package com.printdoc.api.job;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="job_events",indexes=@Index(name="idx_event_job_time",columnList="job_id,created_at"))
public class JobEvent {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="job_id",nullable=false) private PrintJob job;
    @Column(nullable=false,length=32) private String event;
    @Column(nullable=false,length=40) private String actor;
    @Column(length=300) private String note;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected JobEvent(){}
    public JobEvent(PrintJob job,String event,String actor,String note){this.job=job;this.event=event;this.actor=actor;this.note=note;}
    public UUID getId(){return id;} public String getEvent(){return event;} public String getActor(){return actor;} public String getNote(){return note;} public Instant getCreatedAt(){return createdAt;}
}
