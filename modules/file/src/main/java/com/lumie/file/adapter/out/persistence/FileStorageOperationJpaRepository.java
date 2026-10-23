package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.vo.FileStorageOperationStatus;
import com.lumie.file.domain.vo.FileStorageOperationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileStorageOperationJpaRepository extends JpaRepository<FileStorageOperation, UUID> {

    interface MetricSeriesProjection {
        String getOperation();
        String getStatus();
        long getValue();
    }

    @Query(value = """
            SELECT lower(operation_type) AS operation, lower(status) AS status, count(*) AS value
            FROM file_storage_operation
            GROUP BY operation_type, status
            """, nativeQuery = true)
    List<MetricSeriesProjection> operationCounts();

    @Query(value = """
            SELECT lower(operation_type) AS operation, lower(status) AS status,
                   CAST(greatest(0, floor(extract(epoch FROM (:now - min(created_at))))) AS BIGINT) AS value
            FROM file_storage_operation
            WHERE status IN ('PENDING', 'RETRY')
            GROUP BY operation_type, status
            """, nativeQuery = true)
    List<MetricSeriesProjection> oldestOperationAges(@Param("now") Instant now);

    @Query(value = """
            SELECT count(*)
            FROM file_storage_operation
            WHERE operation_type = 'PROMOTE'
              AND status IN ('PENDING', 'RETRY')
              AND deadline_at > :now
              AND deadline_at <= :cutoff
            """, nativeQuery = true)
    long countPromotionsExpiringSoon(@Param("now") Instant now, @Param("cutoff") Instant cutoff);

    boolean existsByFileIdAndOperationTypeAndStatusAndLeaseUntilAfter(
            UUID fileId, FileStorageOperationType operationType, FileStorageOperationStatus status, Instant now);

    @Query(value = """
            SELECT * FROM file_storage_operation
            WHERE (status IN ('PENDING', 'RETRY') AND next_attempt_at <= :now)
               OR (status = 'PROCESSING' AND lease_until <= :now)
            ORDER BY next_attempt_at, created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<FileStorageOperation> lockDueOperations(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
