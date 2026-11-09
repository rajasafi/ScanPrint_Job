package com.printdoc.api.job;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class JobDtos {
    private JobDtos(){}
    public record UploadView(String uploadId,String fileName,String mimeType,long fileSizeBytes,String sha256,Instant expiresAt){}
    public record CreateRequest(@NotBlank @Size(max=80) String idempotencyKey,@NotEmpty @Size(min=1,max=1) List<@NotBlank @Size(max=22) String> uploadIds,@NotBlank @Size(max=32) String pageRange,@Min(1) @Max(99) int copies,@NotNull PrintJob.ColorMode colorMode,@NotNull PrintJob.DuplexMode duplexMode,@NotBlank @Pattern(regexp="A4|A3|LETTER|PHOTO_4X6") String paperSize,@NotNull PrintJob.PaymentMethod paymentMethod){}
    public record JobView(String jobId,String fileName,String mimeType,long fileSizeBytes,int totalPages,String pageRange,int copies,String colorMode,String duplexMode,String paperSize,String paymentMethod,String status,long amountMinor,String currency,Instant createdAt,Instant updatedAt){}
    public record StatusUpdate(@NotNull PrintJob.Status status,@Size(max=300) String note){}
    public record Quote(int pages,int copies,long amountMinor,String currency,String note){}
}
