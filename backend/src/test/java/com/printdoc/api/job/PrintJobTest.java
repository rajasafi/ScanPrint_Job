package com.printdoc.api.job;

import com.printdoc.api.shop.Shop;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PrintJobTest {
    @Test
    void permitsOnlyTheSupportedQueueLifecycle() {
        Shop shop = new Shop("TEST_SHOP", "Sample Shop");
        PrintJob job = new PrintJob("job-1", shop, "file.pdf", 10, "application/pdf", "hash", 1, "all", 1,
            PrintJob.ColorMode.BLACK_WHITE, PrintJob.DuplexMode.SIMPLEX, "A4", PrintJob.PaymentMethod.CASH,
            PrintJob.Status.WAITING_COUNTER_APPROVAL, 200, "idem-1", Instant.now().plusSeconds(3600));

        assertThat(job.canTransitionTo(PrintJob.Status.QUEUED)).isTrue();
        assertThat(job.canTransitionTo(PrintJob.Status.PRINTING)).isFalse();
        job.transition(PrintJob.Status.QUEUED);
        assertThat(job.canTransitionTo(PrintJob.Status.PRINTING)).isTrue();
        assertThat(job.canTransitionTo(PrintJob.Status.AWAITING_PAYMENT)).isFalse();
    }
}
