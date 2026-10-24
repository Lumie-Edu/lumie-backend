package com.lumie.file.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileStorageOperationStatus;
import com.lumie.file.domain.vo.FileUploadStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageOperationProcessorTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final UUID FILE_ID = UUID.fromString("d14b9d11-eaab-4f37-bac5-3d4d205e65d0");
    private static final UUID OPERATION_ID = UUID.fromString("bc2f446f-7633-4e8c-aeae-580734cf4f55");
    private static final String SOURCE = "tmp/demo/uploads/upload-1/notes.pdf";
    private static final String TARGET = "files/demo/announcement/file-1/notes.pdf";

    private final FileStorageOperationPersistencePort operations = mock(FileStorageOperationPersistencePort.class);
    private final StoragePort storage = mock(StoragePort.class);
    private final StorageOperationMetrics metrics = mock(StorageOperationMetrics.class);
    private final StorageOperationProcessor processor = new StorageOperationProcessor(
            operations, storage, Clock.fixed(NOW, ZoneOffset.UTC), metrics);

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void promotionVerifiesDestinationBeforeAtomicallyFinalizingReady() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(SOURCE)).thenReturn(true);
        when(storage.objectExists(TARGET)).thenReturn(false, true);
        when(storage.objectSize(TARGET)).thenReturn(10L);
        when(storage.objectContentType(TARGET)).thenReturn("application/pdf");

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        verify(storage).copyObject(SOURCE, TARGET, "etag-1");
        verify(operations).finalizePromotion(OPERATION_ID, FILE_ID, null);
    }

    @Test
    void alreadyReadyMetadataCompletesReplayWithoutStorageAction() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        FileMetadata metadata = metadata(FileUploadStatus.READY);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));

        processor.processDueOperations(10);

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.COMPLETED);
        verify(operations).save(operation);
        verify(storage, never()).objectExists(anyString());
    }

    @Test
    void missingMetadataDelegatesToStalePromotionPort() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.empty());

        processor.processDueOperations(10);

        verify(operations).finalizeStalePromotion(OPERATION_ID, FILE_ID, null);
        verify(storage, never()).objectExists(anyString());
    }

    @Test
    void expiredTemporarySourceMarksOperationAndMetadataTerminal() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofHours(23)));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.FAILED);
        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.FAILED);
        verify(operations).finalizeFailure(operation, FILE_ID, null,
                "FILE_101", "Temporary object promotion deadline elapsed");
        verify(storage, never()).objectExists(anyString());
    }

    @Test
    void transientStorageFailureSchedulesBoundedRetryWithoutMarkingReady() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(SOURCE)).thenReturn(true);
        when(storage.objectExists(TARGET)).thenReturn(false);
        org.mockito.BDDMockito.willThrow(new BusinessException(com.lumie.file.domain.exception.FileErrorCode.STORAGE_ERROR))
                .given(storage).copyObject(SOURCE, TARGET, "etag-1");

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.RETRY);
        assertThat(operation.getNextAttemptAt()).isAfter(NOW);
        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.PENDING);
        verify(operations).save(operation);
    }

    @Test
    void transientFailureWhoseBackoffCrossesDeadlineFinalizesTerminalState() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        ReflectionTestUtils.setField(operation, "deadlineAt", NOW.plusSeconds(10));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(SOURCE)).thenReturn(true);
        when(storage.objectExists(TARGET)).thenReturn(false);
        org.mockito.BDDMockito.willThrow(new BusinessException(com.lumie.file.domain.exception.FileErrorCode.STORAGE_ERROR))
                .given(storage).copyObject(SOURCE, TARGET, "etag-1");

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.FAILED);
        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.FAILED);
        verify(operations).finalizeFailure(eq(operation), eq(FILE_ID), eq(null), anyString(), anyString());
        verify(operations, never()).save(operation);
        verify(metrics).recordTerminalFailure(operation, "storage_error");
    }

    @Test
    void exponentialRetryBackoffStopsAtTenMinutes() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        for (int attempt = 0; attempt < 6; attempt++) {
            operation.claim("previous-worker", NOW.minus(Duration.ofMinutes(5)), Duration.ofMinutes(2));
        }
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(TARGET)).thenReturn(false);
        when(storage.objectExists(SOURCE)).thenReturn(true);
        org.mockito.BDDMockito.willThrow(new BusinessException(com.lumie.file.domain.exception.FileErrorCode.STORAGE_ERROR))
                .given(storage).copyObject(SOURCE, TARGET, "etag-1");

        processor.processDueOperations(10);

        assertThat(operation.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void reachingAttemptLimitMakesPromotionTerminalWithoutStorageAction() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        for (int attempt = 0; attempt < 10; attempt++) {
            operation.claim("previous-worker", NOW.minus(Duration.ofMinutes(5)), Duration.ofMinutes(2));
        }
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));

        processor.processDueOperations(10);

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.FAILED);
        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.FAILED);
        verify(storage, never()).objectExists(anyString());
    }

    @Test
    void stalePromotionDoesNotCopyOrRestoreAttachmentMarkedForDeletion() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        metadata.markDeleting();
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.DELETING);
        verify(storage, never()).objectExists(anyString());
        verify(operations).finalizeStalePromotion(OPERATION_ID, FILE_ID, null);
    }

    @Test
    void mismatchedPermanentObjectIsTerminalAndNeverMakesMetadataReady() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(TARGET)).thenReturn(true);
        when(storage.objectSize(TARGET)).thenReturn(11L);

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.FAILED);
        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.FAILED);
        verify(operations).finalizeFailure(operation, FILE_ID, null, "FILE_001",
                "Permanent object does not match the registered attachment");
        verify(storage, never()).copyObject(anyString(), anyString(), anyString());
    }

    @Test
    void mismatchedPermanentContentTypeIsTerminal() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(TARGET)).thenReturn(true);
        when(storage.objectSize(TARGET)).thenReturn(10L);
        when(storage.objectContentType(TARGET)).thenReturn("text/plain");

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.FAILED);
        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.FAILED);
        verify(storage, never()).copyObject(anyString(), anyString(), anyString());
    }

    @Test
    void completedCopyReplayVerifiesTargetWithoutCopyingAgain() {
        setTenant();
        FileStorageOperation operation = operation(NOW.minus(Duration.ofMinutes(1)));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(TARGET)).thenReturn(true);
        when(storage.objectSize(TARGET)).thenReturn(10L);
        when(storage.objectContentType(TARGET)).thenReturn("application/pdf");

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        verify(storage, never()).copyObject(anyString(), anyString(), anyString());
        verify(operations).finalizePromotion(OPERATION_ID, FILE_ID, null);
    }

    @Test
    void alreadyAbsentDeleteObjectCompletesIdempotently() {
        setTenant();
        FileStorageOperation operation = deleteOperation();
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        metadata.markDeleting();
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(TARGET)).thenReturn(false);

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        verify(operations).finalizeDeletion(OPERATION_ID, FILE_ID, null);
        verify(storage, never()).deleteObject(TARGET);
        assertThat(metadata.isDeleting()).isTrue();
    }

    @Test
    void successfulDeleteRemovesObjectBeforeFinalizingTombstone() {
        setTenant();
        FileStorageOperation operation = deleteOperation();
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        metadata.markDeleting();
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(TARGET)).thenReturn(true, false);

        assertThat(processor.processDueOperations(10)).isEqualTo(1);

        verify(storage).deleteObject(TARGET);
        verify(operations).finalizeDeletion(OPERATION_ID, FILE_ID, null);
    }

    @Test
    void transientDeleteFailureKeepsTombstoneAndSchedulesRetry() {
        setTenant();
        FileStorageOperation operation = deleteOperation();
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        metadata.markDeleting();
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(TARGET)).thenReturn(true);
        org.mockito.BDDMockito.willThrow(new BusinessException(com.lumie.file.domain.exception.FileErrorCode.STORAGE_ERROR))
                .given(storage).deleteObject(TARGET);

        processor.processDueOperations(10);

        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.DELETING);
        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.RETRY);
        verify(operations).save(operation);
    }

    @Test
    void targetRemainingAfterDeleteIsRetryable() {
        setTenant();
        FileStorageOperation operation = deleteOperation();
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        metadata.markDeleting();
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(storage.objectExists(TARGET)).thenReturn(true, true);

        processor.processDueOperations(10);

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.RETRY);
        verify(storage).deleteObject(TARGET);
        verify(operations).save(operation);
    }

    @Test
    void deleteWaitsForAnActivePromotionLeaseBeforeRemovingTheTarget() {
        setTenant();
        FileStorageOperation operation = deleteOperation();
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        metadata.markDeleting();
        when(operations.claimDueOperations(any(), anyInt(), anyString(), any())).thenReturn(List.of(operation));
        when(operations.findMetadataById(FILE_ID)).thenReturn(Optional.of(metadata));
        when(operations.hasActivePromotionLease(FILE_ID, NOW)).thenReturn(true);

        processor.processDueOperations(10);

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.RETRY);
        assertThat(operation.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(30));
        verify(operations).save(operation);
        verify(storage, never()).objectExists(TARGET);
        verify(operations, never()).finalizeDeletion(OPERATION_ID, FILE_ID, null);
    }

    private static FileStorageOperation operation(Instant sourceCreatedAt) {
        FileStorageOperation operation = FileStorageOperation.promote(
                FILE_ID, SOURCE, TARGET, "etag-1", 10L, "application/pdf", sourceCreatedAt);
        ReflectionTestUtils.setField(operation, "id", OPERATION_ID);
        return operation;
    }

    private static FileStorageOperation deleteOperation() {
        FileStorageOperation operation = FileStorageOperation.delete(FILE_ID, TARGET, NOW.minusSeconds(60));
        ReflectionTestUtils.setField(operation, "id", OPERATION_ID);
        return operation;
    }

    private static FileMetadata metadata(FileUploadStatus status) {
        FileMetadata metadata = FileMetadata.createWithId(FILE_ID, EntityType.ANNOUNCEMENT, 9L,
                "notes.pdf", "application/pdf", 10L, TARGET, null);
        if (status == FileUploadStatus.READY) metadata.markReady();
        if (status == FileUploadStatus.FAILED) metadata.markFailed();
        return metadata;
    }

    private static void setTenant() {
        TenantContextHolder.setTenant("inst-demo");
        TenantContextHolder.setTenantId(42L);
    }
}
