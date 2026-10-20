package com.lumie.file.adapter.in.internal;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.api.PreparedTemporaryFile;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.FileStorageOperationType;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileUploadStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FileServiceAdapterTest {

    private static final UUID FILE_ID = UUID.fromString("d14b9d11-eaab-4f37-bac5-3d4d205e65d0");
    private static final String SOURCE_KEY = "tmp/demo/uploads/upload-1/notes.pdf";
    private static final String TARGET_KEY = "files/demo/announcement/file-1/notes.pdf";

    @Mock private FileMetadataRepository fileMetadataRepository;
    @Mock private StoragePort storagePort;
    @Mock private FileStorageOperationPersistencePort operationPersistencePort;
    @InjectMocks private FileServiceAdapter adapter;

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenant("inst-demo");
        TenantContextHolder.setTenantId(42L);
        lenient().when(fileMetadataRepository.save(any())).thenAnswer(invocation -> {
            FileMetadata metadata = invocation.getArgument(0);
            ReflectionTestUtils.setField(metadata, "id", FILE_ID);
            return metadata;
        });
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void attachedMetadataStartsPendingAndKeepsLegacyCompletionFalse() {
        adapter.attachPreparedFiles("announcement", 9L, List.of(preparedFile()));

        ArgumentCaptor<FileMetadata> metadata = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileMetadataRepository).save(metadata.capture());
        assertThat(metadata.getValue().getUploadStatus()).isEqualTo(FileUploadStatus.PENDING);
        assertThat(metadata.getValue().isUploadCompleted()).isFalse();
    }

    @Test
    void attachedMetadataCreatesOneMatchingPromotionIntent() {
        adapter.attachPreparedFiles("announcement", 9L, List.of(preparedFile()));

        ArgumentCaptor<FileStorageOperation> operation = ArgumentCaptor.forClass(FileStorageOperation.class);
        verify(operationPersistencePort).save(operation.capture());
        assertThat(operation.getValue().getFileId()).isEqualTo(FILE_ID);
        assertThat(operation.getValue().getOperationType()).isEqualTo(FileStorageOperationType.PROMOTE);
        assertThat(operation.getValue().getSourceObjectKey()).isEqualTo(SOURCE_KEY);
        assertThat(operation.getValue().getTargetObjectKey()).isEqualTo(TARGET_KEY);
        assertThat(operation.getValue().getExpectedSize()).isEqualTo(10L);
        assertThat(operation.getValue().getExpectedContentType()).isEqualTo("application/pdf");
    }

    @Test
    void deletingFilesByEntityMarksTombstonesAndCreatesDeleteIntentsWithoutStorageCalls() {
        UUID secondId = UUID.randomUUID();
        FileMetadata first = metadata(FILE_ID, 9L, "files/demo/announcement/one.pdf");
        FileMetadata second = metadata(secondId, 9L, "files/demo/announcement/two.pdf");
        given(fileMetadataRepository.findByEntityTypeAndEntityId(
                com.lumie.file.domain.vo.EntityType.ANNOUNCEMENT, 9L)).willReturn(List.of(first, second));

        adapter.deleteFilesByEntity("announcement", 9L);

        assertThat(first.isDeleting()).isTrue();
        assertThat(second.isDeleting()).isTrue();
        ArgumentCaptor<FileStorageOperation> operations = ArgumentCaptor.forClass(FileStorageOperation.class);
        verify(operationPersistencePort, org.mockito.Mockito.times(2)).save(operations.capture());
        assertThat(operations.getAllValues()).allSatisfy(operation ->
                assertThat(operation.getOperationType()).isEqualTo(FileStorageOperationType.DELETE));
        verifyNoInteractions(storagePort);
        verify(fileMetadataRepository, never()).deleteById(FILE_ID);
    }

    @Test
    void deletingSpecificOwnedIdsCreatesOnlyMatchingDeleteIntent() {
        FileMetadata owned = metadata(FILE_ID, 9L, "files/demo/announcement/one.pdf");
        given(fileMetadataRepository.findById(FILE_ID)).willReturn(java.util.Optional.of(owned));

        adapter.deleteFilesByEntityAndIds("announcement", 9L, List.of(FILE_ID));

        ArgumentCaptor<FileStorageOperation> operation = ArgumentCaptor.forClass(FileStorageOperation.class);
        verify(operationPersistencePort).save(operation.capture());
        assertThat(operation.getValue().getFileId()).isEqualTo(FILE_ID);
        assertThat(operation.getValue().getTargetObjectKey()).isEqualTo("files/demo/announcement/one.pdf");
        assertThat(owned.isDeleting()).isTrue();
        verifyNoInteractions(storagePort);
    }

    @Test
    void deletingIdsCreatesDeleteIntentForEachNonTextbookFile() {
        FileMetadata file = metadata(FILE_ID, 9L, "files/demo/announcement/one.pdf");
        given(fileMetadataRepository.findById(FILE_ID)).willReturn(java.util.Optional.of(file));

        adapter.deleteFilesByIds(List.of(FILE_ID));

        ArgumentCaptor<FileStorageOperation> operation = ArgumentCaptor.forClass(FileStorageOperation.class);
        verify(operationPersistencePort).save(operation.capture());
        assertThat(operation.getValue().getOperationType()).isEqualTo(FileStorageOperationType.DELETE);
        assertThat(file.isDeleting()).isTrue();
        verifyNoInteractions(storagePort);
    }

    @Test
    void invalidRetainedIdIsRejectedBeforeAnyDeleteIntentIsCreated() {
        FileMetadata current = metadata(FILE_ID, 9L, "files/demo/announcement/current.pdf");
        given(fileMetadataRepository.findByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 9L))
                .willReturn(List.of(current));
        UUID foreignId = UUID.randomUUID();

        assertThatThrownBy(() -> adapter.reconcilePreparedDirectFiles(
                "announcement", 9L, List.of(foreignId), List.of()))
                .isInstanceOf(BusinessException.class);

        assertThat(current.isReady()).isTrue();
        verifyNoInteractions(operationPersistencePort, storagePort);
    }

    private static FileMetadata metadata(UUID id, Long entityId, String objectKey) {
        FileMetadata metadata = FileMetadata.createWithId(id, EntityType.ANNOUNCEMENT, entityId,
                "notes.pdf", "application/pdf", 10L, objectKey, null);
        metadata.markReady();
        return metadata;
    }

    private static PreparedTemporaryFile preparedFile() {
        return new PreparedTemporaryFile(
                FILE_ID,
                "notes.pdf",
                "application/pdf",
                10L,
                SOURCE_KEY,
                TARGET_KEY,
                "source-etag",
                Instant.parse("2026-10-20T00:00:00Z"));
    }
}
