package com.lumie.file.adapter.out.persistence;

import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.repository.FileMetadataRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FileStorageOperationPersistenceAdapter implements FileStorageOperationPersistencePort {

    private final FileStorageOperationJpaRepository operationRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @Override
    @Transactional
    public FileStorageOperation save(FileStorageOperation operation) {
        return operationRepository.save(operation);
    }

    @Override
    @Transactional
    public List<FileStorageOperation> claimDueOperations(Instant now, int batchSize,
                                                         String leaseOwner, Duration leaseDuration) {
        List<FileStorageOperation> operations = operationRepository.lockDueOperations(now, batchSize);
        operations.forEach(operation -> operation.claim(leaseOwner, now, leaseDuration));
        return operationRepository.saveAll(operations);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileMetadata> findMetadataById(UUID fileId) {
        return fileMetadataRepository.findById(fileId);
    }

    @Override
    @Transactional
    public void finalizePromotion(UUID operationId, UUID fileId, String leaseOwner) {
        FileStorageOperation operation = operationRepository.findById(operationId).orElseThrow();
        if (operation.getStatus() != com.lumie.file.domain.vo.FileStorageOperationStatus.PROCESSING
                || !Objects.equals(operation.getLeaseOwner(), leaseOwner)) {
            return;
        }
        FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
        Instant now = Instant.now();
        if (metadata != null && metadata.isReady()) {
            operation.markCompleted(now);
            return;
        }
        if (metadata != null && metadata.getUploadStatus() == com.lumie.file.domain.vo.FileUploadStatus.PENDING) {
            metadata.markReady();
            fileMetadataRepository.save(metadata);
        } else {
            // A stale promotion may have copied after deletion began or metadata was purged.
            // Preserve a fresh durable delete intent so the object cannot become orphaned.
            operationRepository.save(FileStorageOperation.delete(fileId, operation.getTargetObjectKey(), now));
        }
        operation.markCompleted(now);
    }

    @Override
    @Transactional
    public void finalizeStalePromotion(UUID operationId, UUID fileId, String leaseOwner) {
        FileStorageOperation operation = operationRepository.findById(operationId).orElseThrow();
        if (operation.getStatus() != com.lumie.file.domain.vo.FileStorageOperationStatus.PROCESSING
                || !Objects.equals(operation.getLeaseOwner(), leaseOwner)) {
            return;
        }
        FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
        Instant now = Instant.now();
        if (metadata != null && metadata.getUploadStatus() == com.lumie.file.domain.vo.FileUploadStatus.PENDING) {
            // A read outside the RLS-bound adapter once hid a pending row and marked it READY without copying.
            operation.scheduleRetry(now, now.plusSeconds(30), "PENDING_METADATA_VISIBLE",
                    "Pending attachment must be copied and verified before promotion completes");
            operationRepository.save(operation);
            return;
        }
        if (metadata == null || !metadata.isReady()) {
            operationRepository.save(FileStorageOperation.delete(fileId, operation.getTargetObjectKey(), now));
        }
        operation.markCompleted(now);
    }

    @Override
    @Transactional
    public void finalizeDeletion(UUID operationId, UUID fileId, String leaseOwner) {
        FileStorageOperation operation = operationRepository.findById(operationId).orElseThrow();
        if (operation.getStatus() != com.lumie.file.domain.vo.FileStorageOperationStatus.PROCESSING
                || !Objects.equals(operation.getLeaseOwner(), leaseOwner)) {
            return;
        }
        operation.markCompleted(Instant.now());
        fileMetadataRepository.findById(fileId)
                .filter(FileMetadata::isDeleting)
                .ifPresent(fileMetadataRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActivePromotionLease(UUID fileId, Instant now) {
        return operationRepository.existsByFileIdAndOperationTypeAndStatusAndLeaseUntilAfter(
                fileId, com.lumie.file.domain.vo.FileStorageOperationType.PROMOTE,
                com.lumie.file.domain.vo.FileStorageOperationStatus.PROCESSING, now);
    }

    @Override
    @Transactional
    public void finalizeFailure(FileStorageOperation failedOperation, UUID fileId, String leaseOwner,
                                String errorCode, String message) {
        FileStorageOperation operation = operationRepository.findById(failedOperation.getId()).orElseThrow();
        if (operation.getStatus() != com.lumie.file.domain.vo.FileStorageOperationStatus.PROCESSING
                || !Objects.equals(operation.getLeaseOwner(), leaseOwner)) {
            return;
        }
        FileMetadata metadata = fileMetadataRepository.findById(fileId).orElse(null);
        Instant now = Instant.now();
        operation.markFailed(now, errorCode, message);
        if (metadata != null && metadata.getUploadStatus() == com.lumie.file.domain.vo.FileUploadStatus.PENDING) {
            metadata.markFailed();
            fileMetadataRepository.save(metadata);
        }
    }
}
