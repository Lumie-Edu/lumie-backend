package com.lumie.file.application.port.out;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for durable MinIO promotion and deletion intent. */
public interface FileStorageOperationPersistencePort {

    FileStorageOperation save(FileStorageOperation operation);

    List<FileStorageOperation> claimDueOperations(Instant now, int batchSize, String leaseOwner, Duration leaseDuration);

    /** Load inside an RLS-bound transaction when called from the storage scheduler. */
    Optional<FileMetadata> findMetadataById(UUID fileId);

    void finalizePromotion(UUID operationId, UUID fileId, String leaseOwner);

    void finalizeStalePromotion(UUID operationId, UUID fileId, String leaseOwner);

    void finalizeDeletion(UUID operationId, UUID fileId, String leaseOwner);

    boolean hasActivePromotionLease(UUID fileId, Instant now);

    void finalizeFailure(FileStorageOperation operation, UUID fileId, String leaseOwner,
                         String errorCode, String message);
}
