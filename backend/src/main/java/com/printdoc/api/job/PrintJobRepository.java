package com.printdoc.api.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface PrintJobRepository extends JpaRepository<PrintJob,UUID> {
    Page<PrintJob> findByShopIdOrderByCreatedAtDesc(UUID shopId, Pageable pageable);
    Page<PrintJob> findByShopIdAndStatusOrderByCreatedAtDesc(UUID shopId, PrintJob.Status status, Pageable pageable);
    long countByShopId(UUID shopId);
    Optional<PrintJob> findByPublicJobIdAndShopId(String publicJobId, UUID shopId);
    Optional<PrintJob> findByIdempotencyKeyAndShopId(String idempotencyKey, UUID shopId);
    boolean existsByShopIdAndIdempotencyKey(UUID shopId,String idempotencyKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from PrintJob j where j.publicJobId=:jobId and j.shop.id=:shopId")
    Optional<PrintJob> lockByPublicIdAndShop(@Param("jobId") String jobId,@Param("shopId") UUID shopId);
}
