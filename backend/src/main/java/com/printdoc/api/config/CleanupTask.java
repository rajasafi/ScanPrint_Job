package com.printdoc.api.config;

import com.printdoc.api.job.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanupTask {
    private static final Logger log=LoggerFactory.getLogger(CleanupTask.class); private final JobService jobs;
    public CleanupTask(JobService jobs){this.jobs=jobs;}
    @Scheduled(fixedDelayString="${printdoc.cleanup.interval-ms:600000}") public void purgeExpiredUploads(){int removed=jobs.purgeExpired();if(removed>0)log.info("Removed {} expired temporary document uploads",removed);}
}
