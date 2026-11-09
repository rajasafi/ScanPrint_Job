package com.printdoc.api.job;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument,UUID> {
    Optional<UploadedDocument> findByUploadIdAndShopId(String uploadId,UUID shopId);
    List<UploadedDocument> findByPublicJobId(String publicJobId);
    List<UploadedDocument> findByExpiresAtBefore(Instant instant);
}
