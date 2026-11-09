package com.printdoc.api.socket;

import com.printdoc.api.job.JobDtos;

/** Outbound agent events. The no-op implementation keeps REST-only deployments safe. */
public interface AgentSocketPublisher {
    void publishJobStatus(String shopCode,JobDtos.JobView job);
    void dispatch(String shopCode,Object jobPayload);
    void cancel(String shopCode,String jobId);
    void requestPrinterScan(String shopCode);
}
