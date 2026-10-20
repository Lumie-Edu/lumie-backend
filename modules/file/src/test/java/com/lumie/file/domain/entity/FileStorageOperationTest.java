package com.lumie.file.domain.entity;

import com.lumie.file.domain.vo.FileStorageOperationStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageOperationTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    @Test
    void expiredProcessingLeaseCanBeClaimedAgain() {
        FileStorageOperation operation = operation();
        operation.claim("worker-a", NOW.minus(Duration.ofMinutes(5)), Duration.ofMinutes(2));

        assertThat(operation.isRetryable(NOW)).isTrue();

        operation.claim("worker-b", NOW, Duration.ofMinutes(2));
        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.PROCESSING);
        assertThat(operation.getAttemptCount()).isEqualTo(2);
        assertThat(operation.getLeaseOwner()).isEqualTo("worker-b");
        assertThat(operation.isRetryable(NOW.plus(Duration.ofSeconds(30)))).isFalse();
    }

    @Test
    void retryIsScheduledBeforeTheSourceSafetyDeadline() {
        FileStorageOperation operation = operation();
        operation.claim("worker-a", NOW, Duration.ofMinutes(2));
        operation.scheduleRetry(NOW, NOW.plusSeconds(30), "STORAGE_ERROR", "temporary outage");

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.RETRY);
        assertThat(operation.getNextAttemptAt()).isBefore(operation.getDeadlineAt());
        assertThat(operation.getLastErrorCode()).isEqualTo("STORAGE_ERROR");
    }

    @Test
    void promotionFactoryUsesObservedTimeWhenSourceTimestampIsMissing() {
        FileStorageOperation operation = FileStorageOperation.promote(
                UUIDs.FILE_ID, "tmp/source", "files/target", "etag", 10L,
                "application/pdf", null);

        assertThat(operation.getSourceCreatedAt()).isNotNull();
        assertThat(operation.getSourceExpiresAt()).isEqualTo(operation.getSourceCreatedAt().plus(Duration.ofDays(1)));
        assertThat(operation.getDeadlineAt()).isEqualTo(operation.getSourceExpiresAt().minus(Duration.ofHours(2)));
    }

    @Test
    void retryAfterTheDeadlineBecomesTerminal() {
        FileStorageOperation operation = operation();
        operation.claim("worker-a", NOW, Duration.ofMinutes(2));

        operation.scheduleRetry(operation.getDeadlineAt(), operation.getDeadlineAt().plusSeconds(30),
                "STORAGE_ERROR", "deadline elapsed");

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.FAILED);
        assertThat(operation.getCompletedAt()).isEqualTo(operation.getDeadlineAt());
    }

    @Test
    void duePendingOperationIsRetryableButFutureOperationIsNot() {
        FileStorageOperation operation = operation();
        org.springframework.test.util.ReflectionTestUtils.setField(operation, "nextAttemptAt", NOW);
        assertThat(operation.isRetryable(NOW)).isTrue();
        assertThat(operation.isRetryable(NOW.minusSeconds(1))).isFalse();
    }

    @Test
    void dueRetryIsRetryableAndAProcessingOperationWithoutLeaseCannotBeReclaimed() {
        FileStorageOperation operation = operation();
        operation.claim("worker-a", NOW, Duration.ofMinutes(2));
        operation.scheduleRetry(NOW, NOW.plusSeconds(30), "STORAGE_ERROR", "retry");

        assertThat(operation.isRetryable(NOW.plusSeconds(29))).isFalse();
        assertThat(operation.isRetryable(NOW.plusSeconds(30))).isTrue();

        operation.claim("worker-b", NOW.plusSeconds(30), Duration.ofMinutes(2));
        org.springframework.test.util.ReflectionTestUtils.setField(operation, "leaseUntil", null);
        assertThat(operation.isRetryable(NOW.plusSeconds(31))).isFalse();
    }

    @Test
    void deleteFactoryUsesCurrentTimeWhenNoTimestampIsProvided() {
        FileStorageOperation operation = FileStorageOperation.delete(UUIDs.FILE_ID, "files/key", null);

        assertThat(operation.getOperationType()).isEqualTo(com.lumie.file.domain.vo.FileStorageOperationType.DELETE);
        assertThat(operation.getDeadlineAt()).isAfter(operation.getNextAttemptAt());
        assertThat(operation.getSourceObjectKey()).isNull();
    }

    private static FileStorageOperation operation() {
        return FileStorageOperation.promote(UUIDs.FILE_ID, "tmp/source", "files/target",
                "etag", 10L, "application/pdf", NOW.minus(Duration.ofHours(1)));
    }

    private static final class UUIDs {
        private static final java.util.UUID FILE_ID =
                java.util.UUID.fromString("d14b9d11-eaab-4f37-bac5-3d4d205e65d0");
    }
}
