package com.printdoc.api.job;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JobEventRepository extends JpaRepository<JobEvent,UUID> {
    List<JobEvent> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}
