package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileStorageOperationStatus;
import com.lumie.file.domain.vo.FileStorageOperationType;
import com.lumie.file.domain.vo.FileUploadStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageOperationPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final UUID FILE_ID = UUID.fromString("d14b9d11-eaab-4f37-bac5-3d4d205e65d0");
    private static final UUID OPERATION_ID = UUID.fromString("bc2f446f-7633-4e8c-aeae-580734cf4f55");

    private final FileStorageOperationJpaRepository operationRepository = mock(FileStorageOperationJpaRepository.class);
    private final FileMetadataRepository fileRepository = mock(FileMetadataRepository.class);
    private final FileStorageOperationPersistenceAdapter adapter =
            new FileStorageOperationPersistenceAdapter(operationRepository, fileRepository);

    @Test
    void claimSetsRecoverableLeaseAndIncrementsAttemptCount() {
        FileStorageOperation operation = operation();
        when(operationRepository.lockDueOperations(NOW, 10)).thenReturn(List.of(operation));
        when(operationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FileStorageOperation> claimed = adapter.claimDueOperations(
                NOW, 10, "worker-a", Duration.ofMinutes(2));

        assertThat(claimed).containsExactly(operation);
        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.PROCESSING);
        assertThat(operation.getAttemptCount()).isEqualTo(1);
        assertThat(operation.getLeaseOwner()).isEqualTo("worker-a");
        assertThat(operation.getLeaseUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
    }

    @Test
    void verifiedPromotionUpdatesReadyAndCompletesOperationInOnePersistenceBoundary() {
        FileStorageOperation operation = operation();
        operation.claim("worker-a", NOW, Duration.ofMinutes(2));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        when(operationRepository.findById(OPERATION_ID)).thenReturn(Optional.of(operation));
        when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(metadata));

        adapter.finalizePromotion(OPERATION_ID, FILE_ID, "worker-a");

        assertThat(metadata.isReady()).isTrue();
        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.COMPLETED);
        verify(fileRepository).save(metadata);
    }

    @Test
    void stalePromotionAfterDeleteKeepsTombstoneAndCreatesFollowupDeleteIntent() {
        FileStorageOperation operation = operation();
        operation.claim("worker-a", NOW, Duration.ofMinutes(2));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        metadata.markDeleting();
        when(operationRepository.findById(OPERATION_ID)).thenReturn(Optional.of(operation));
        when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(metadata));

        adapter.finalizePromotion(OPERATION_ID, FILE_ID, "worker-a");

        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.DELETING);
        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.COMPLETED);
        verify(operationRepository).save(org.mockito.ArgumentMatchers.argThat(delete ->
                delete.getOperationType() == FileStorageOperationType.DELETE
                        && delete.getFileId().equals(FILE_ID)));
        verify(fileRepository, never()).save(metadata);
    }

    @Test
    void expiredWorkerCannotFinalizeAnotherWorkersLease() {
        FileStorageOperation operation = operation();
        operation.claim("worker-b", NOW, Duration.ofMinutes(2));
        when(operationRepository.findById(OPERATION_ID)).thenReturn(Optional.of(operation));

        adapter.finalizePromotion(OPERATION_ID, FILE_ID, "worker-a");

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.PROCESSING);
        verify(fileRepository, never()).findById(any());
        verify(operationRepository, never()).save(any(FileStorageOperation.class));
    }

    @Test
    void deleteFinalizationRemovesOnlyTheUnavailableTombstone() {
        FileStorageOperation operation = FileStorageOperation.delete(FILE_ID, "files/demo/notes.pdf", NOW);
        ReflectionTestUtils.setField(operation, "id", OPERATION_ID);
        operation.claim("worker-a", NOW, Duration.ofMinutes(2));
        FileMetadata metadata = metadata(FileUploadStatus.PENDING);
        metadata.markDeleting();
        when(operationRepository.findById(OPERATION_ID)).thenReturn(Optional.of(operation));
        when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(metadata));

        adapter.finalizeDeletion(OPERATION_ID, FILE_ID, "worker-a");

        assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.COMPLETED);
        verify(fileRepository).delete(metadata);
    }

    private static FileStorageOperation operation() {
        FileStorageOperation operation = FileStorageOperation.promote(
                FILE_ID, "tmp/demo/notes.pdf", "files/demo/notes.pdf", "etag-1", 10L,
                "application/pdf", NOW.minus(Duration.ofMinutes(1)));
        ReflectionTestUtils.setField(operation, "id", OPERATION_ID);
        return operation;
    }

    private static FileMetadata metadata(FileUploadStatus status) {
        FileMetadata metadata = FileMetadata.createWithId(FILE_ID, EntityType.ANNOUNCEMENT, 9L,
                "notes.pdf", "application/pdf", 10L, "files/demo/notes.pdf", null);
        if (status == FileUploadStatus.READY) metadata.markReady();
        if (status == FileUploadStatus.FAILED) metadata.markFailed();
        return metadata;
    }
}
