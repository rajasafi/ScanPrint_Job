package com.printdoc.api.socket;

import com.printdoc.api.job.JobDtos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="printdoc.socket.enabled",havingValue="false",matchIfMissing=true)
public class NoopAgentSocketPublisher implements AgentSocketPublisher {
    public void publishJobStatus(String shopCode,JobDtos.JobView job) {}
    public void dispatch(String shopCode,Object jobPayload) {}
    public void cancel(String shopCode,String jobId) {}
    public void requestPrinterScan(String shopCode) {}
}
