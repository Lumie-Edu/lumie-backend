package com.lumie.file.application.service;

import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.vo.FileStorageOperationStatus;
import com.lumie.file.domain.vo.FileStorageOperationType;
import com.lumie.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class StorageOperationProcessor {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(2);

    private final FileStorageOperationPersistencePort operationPersistencePort;
    private final StoragePort storagePort;
    private final StorageOperationMetrics metrics;
    private final Clock clock;

    public StorageOperationProcessor(FileStorageOperationPersistencePort operationPersistencePort,
                                     StoragePort storagePort,
                                     @Nullable Clock clock, StorageOperationMetrics metrics) {
        this.operationPersistencePort = operationPersistencePort;
        this.storagePort = storagePort;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.metrics = metrics;
    }

    public int processDueOperations() {
        return processDueOperations(DEFAULT_BATCH_SIZE);
    }

    public int processDueOperations(int batchSize) {
        return processDueOperations(batchSize, UUID.randomUUID().toString());
    }

    public int processDueOperations(int batchSize, String leaseOwner) {
        Instant now = clock.instant();
        List<FileStorageOperation> due = operationPersistencePort.claimDueOperations(
                now, batchSize, leaseOwner, LEASE_DURATION);
        for (FileStorageOperation operation : due) {
            FileMetadata metadata = operationPersistencePort.findMetadataById(operation.getFileId()).orElse(null);
            try {
                process(operation, metadata, clock.instant());
            } catch (RuntimeException exception) {
                recordFailure(operation, metadata, exception, clock.instant(), false);
            }
        }
        return due.size();
    }

    private void process(FileStorageOperation operation, FileMetadata metadata, Instant now) {
        if (operation.getOperationType() == FileStorageOperationType.DELETE) {
            processDelete(operation, metadata, now);
            return;
        }
        if (metadata != null && metadata.isReady()) {
            operation.markCompleted(now);
            operationPersistencePort.save(operation);
            return;
        }
        if (metadata == null || metadata.getUploadStatus() != com.lumie.file.domain.vo.FileUploadStatus.PENDING) {
            operationPersistencePort.finalizeStalePromotion(
                    operation.getId(), operation.getFileId(), operation.getLeaseOwner());
            return;
        }
        if (!now.isBefore(operation.getDeadlineAt())) {
            recordFailure(operation, metadata, new BusinessException(FileErrorCode.FILE_NOT_FOUND,
                    "Temporary object promotion deadline elapsed"), now, true);
            return;
        }
        if (operation.getAttemptCount() >= MAX_ATTEMPTS) {
            recordFailure(operation, metadata, new BusinessException(FileErrorCode.STORAGE_ERROR,
                    "Storage operation attempt limit reached"), now, true);
            return;
        }
        boolean targetExists = storagePort.objectExists(operation.getTargetObjectKey());
        if (!targetExists) {
            if (operation.getSourceObjectKey() == null || !storagePort.objectExists(operation.getSourceObjectKey())) {
                throw new BusinessException(FileErrorCode.FILE_NOT_FOUND, "Temporary source object is missing");
            }
            storagePort.copyObject(operation.getSourceObjectKey(), operation.getTargetObjectKey(), operation.getSourceEtag());
        }
        verifyPermanentObject(operation);
        operationPersistencePort.finalizePromotion(
                operation.getId(), operation.getFileId(), operation.getLeaseOwner());
        metrics.recordAttempt(operation, "success");
    }

    private void processDelete(FileStorageOperation operation, FileMetadata metadata, Instant now) {
        if (!now.isBefore(operation.getDeadlineAt())) {
            recordFailure(operation, metadata, new BusinessException(FileErrorCode.STORAGE_ERROR,
                    "Object deletion deadline elapsed"), now, true);
            return;
        }
        if (operation.getAttemptCount() >= MAX_ATTEMPTS) {
            recordFailure(operation, metadata, new BusinessException(FileErrorCode.STORAGE_ERROR,
                    "Storage operation attempt limit reached"), now, true);
            return;
        }
        if (operationPersistencePort.hasActivePromotionLease(operation.getFileId(), now)) {
            operation.scheduleRetry(now, now.plusSeconds(30), "PROMOTION_IN_FLIGHT",
                    "Deletion waits for the active promotion lease to finish");
            operationPersistencePort.save(operation);
            metrics.recordAttempt(operation, "retry");
            return;
        }
        if (storagePort.objectExists(operation.getTargetObjectKey())) {
            storagePort.deleteObject(operation.getTargetObjectKey());
        }
        if (storagePort.objectExists(operation.getTargetObjectKey())) {
            throw new BusinessException(FileErrorCode.STORAGE_ERROR, "Object remains after delete request");
        }
        operationPersistencePort.finalizeDeletion(
                operation.getId(), operation.getFileId(), operation.getLeaseOwner());
        metrics.recordAttempt(operation, "success");
    }

    private void verifyPermanentObject(FileStorageOperation operation) {
        if (!storagePort.objectExists(operation.getTargetObjectKey())
                || storagePort.objectSize(operation.getTargetObjectKey()) != operation.getExpectedSize()
                || operation.getExpectedContentType() == null
                || !operation.getExpectedContentType().equalsIgnoreCase(
                        storagePort.objectContentType(operation.getTargetObjectKey()))) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA,
                    "Permanent object does not match the registered attachment");
        }
    }

    private void recordFailure(FileStorageOperation operation, FileMetadata metadata, RuntimeException exception,
                               Instant now, boolean forceTerminal) {
        FileErrorCode code = exception instanceof BusinessException businessException
                && businessException.getErrorCode() instanceof FileErrorCode fileError
                ? fileError : FileErrorCode.STORAGE_ERROR;
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        String leaseOwner = operation.getLeaseOwner();
        if (forceTerminal || code == FileErrorCode.FILE_NOT_FOUND || code == FileErrorCode.INVALID_FILE_DATA) {
            operation.markFailed(now, code.getCode(), message);
            if (metadata != null && metadata.getUploadStatus()
                    == com.lumie.file.domain.vo.FileUploadStatus.PENDING) {
                metadata.markFailed();
            }
            operationPersistencePort.finalizeFailure(
                    operation, operation.getFileId(), leaseOwner, code.getCode(), message);
            metrics.recordAttempt(operation, "terminal");
            metrics.recordTerminalFailure(operation, terminalReason(code, message));
            return;
        }
        long delaySeconds = Math.min(600L, 30L << Math.min(Math.max(operation.getAttemptCount() - 1, 0), 5));
        operation.scheduleRetry(now, now.plusSeconds(delaySeconds), code.getCode(), message);
        if (operation.getStatus() == FileStorageOperationStatus.FAILED) {
            if (metadata != null && metadata.getUploadStatus()
                    == com.lumie.file.domain.vo.FileUploadStatus.PENDING) {
                metadata.markFailed();
            }
            operationPersistencePort.finalizeFailure(
                    operation, operation.getFileId(), leaseOwner, code.getCode(), message);
            metrics.recordAttempt(operation, "terminal");
            metrics.recordTerminalFailure(operation, terminalReason(code, message));
            return;
        }
        operationPersistencePort.save(operation);
        metrics.recordAttempt(operation, "retry");
    }

    private static String terminalReason(FileErrorCode code, String message) {
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("attempt limit")) return "attempt_limit";
        if (normalized.contains("deadline") || normalized.contains("expired")) return "retry_deadline";
        if (code == FileErrorCode.FILE_NOT_FOUND) return "source_missing";
        if (code == FileErrorCode.INVALID_FILE_DATA) return "metadata_mismatch";
        return "storage_error";
    }
}
