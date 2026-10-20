package com.lumie.file.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.file.domain.vo.FileStorageOperationStatus;
import com.lumie.file.domain.vo.FileStorageOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Durable, tenant-scoped intent for an asynchronous object-storage operation. */
@Entity
@Table(name = "file_storage_operation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileStorageOperation extends TenantScopedEntity {

    public static FileStorageOperation promote(UUID fileId, String sourceKey, String targetKey,
                                                String sourceEtag, long expectedSize, String contentType,
                                                Instant sourceCreatedAt) {
        Instant created = sourceCreatedAt == null ? Instant.now() : sourceCreatedAt;
        Instant expires = created.plus(Duration.ofDays(1));
        FileStorageOperation operation = new FileStorageOperation();
        operation.fileId = fileId;
        operation.operationType = FileStorageOperationType.PROMOTE;
        operation.status = FileStorageOperationStatus.PENDING;
        operation.sourceObjectKey = sourceKey;
        operation.targetObjectKey = targetKey;
        operation.sourceEtag = sourceEtag;
        operation.expectedSize = expectedSize;
        operation.expectedContentType = contentType;
        operation.sourceCreatedAt = created;
        operation.sourceExpiresAt = expires;
        operation.attemptCount = 0;
        operation.nextAttemptAt = Instant.now();
        operation.deadlineAt = expires.minus(Duration.ofHours(2));
        return operation;
    }

    public static FileStorageOperation delete(UUID fileId, String targetKey, Instant createdAt) {
        Instant created = createdAt == null ? Instant.now() : createdAt;
        FileStorageOperation operation = new FileStorageOperation();
        operation.fileId = fileId;
        operation.operationType = FileStorageOperationType.DELETE;
        operation.status = FileStorageOperationStatus.PENDING;
        operation.targetObjectKey = targetKey;
        operation.attemptCount = 0;
        operation.nextAttemptAt = created;
        operation.deadlineAt = created.plus(Duration.ofDays(7));
        return operation;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 16)
    private FileStorageOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileStorageOperationStatus status;

    @Column(name = "source_object_key", length = 500)
    private String sourceObjectKey;

    @Column(name = "target_object_key", nullable = false, length = 500)
    private String targetObjectKey;

    @Column(name = "source_etag", length = 255)
    private String sourceEtag;

    @Column(name = "expected_size")
    private Long expectedSize;

    @Column(name = "expected_content_type", length = 100)
    private String expectedContentType;

    @Column(name = "source_created_at")
    private Instant sourceCreatedAt;

    @Column(name = "source_expires_at")
    private Instant sourceExpiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Column(name = "lease_owner", length = 120)
    private String leaseOwner;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "last_error_code", length = 80)
    private String lastErrorCode;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "completed_at")
    private Instant completedAt;

    public void claim(String owner, Instant now, Duration leaseDuration) {
        this.status = FileStorageOperationStatus.PROCESSING;
        this.attemptCount++;
        this.leaseOwner = owner;
        this.leaseUntil = now.plus(leaseDuration);
    }

    public boolean isRetryable(Instant now) {
        return ((status == FileStorageOperationStatus.PENDING || status == FileStorageOperationStatus.RETRY)
                && !nextAttemptAt.isAfter(now))
                || (status == FileStorageOperationStatus.PROCESSING
                && leaseUntil != null && !leaseUntil.isAfter(now));
    }

    public void markCompleted(Instant now) {
        this.status = FileStorageOperationStatus.COMPLETED;
        this.completedAt = now;
        this.leaseOwner = null;
        this.leaseUntil = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markFailed(Instant now, String code, String message) {
        this.status = FileStorageOperationStatus.FAILED;
        this.completedAt = now;
        this.leaseOwner = null;
        this.leaseUntil = null;
        this.lastErrorCode = code;
        this.lastErrorMessage = message;
    }

    public void scheduleRetry(Instant now, Instant nextAttempt, String code, String message) {
        if (!nextAttempt.isBefore(deadlineAt) || !now.isBefore(deadlineAt)) {
            markFailed(now, code, message);
            return;
        }
        this.status = FileStorageOperationStatus.RETRY;
        this.nextAttemptAt = nextAttempt;
        this.leaseOwner = null;
        this.leaseUntil = null;
        this.lastErrorCode = code;
        this.lastErrorMessage = message;
    }
}
