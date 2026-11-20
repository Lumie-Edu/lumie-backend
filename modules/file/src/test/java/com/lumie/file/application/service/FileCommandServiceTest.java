package com.lumie.file.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.application.port.out.FileDownloadPersistencePort;
import com.lumie.file.application.port.out.FileMetadataPersistencePort;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileStorageOperationType;
import com.lumie.file.domain.vo.FileUploadStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class FileCommandServiceTest {

    private static final UUID FILE_ID = UUID.fromString("d14b9d11-eaab-4f37-bac5-3d4d205e65d0");
    private static final String OBJECT_KEY = "files/demo/textbook/file-1/notes.pdf";

    @Mock private FileMetadataPersistencePort fileMetadataRepository;
    @Mock private FileDownloadPersistencePort fileDownloadRepository;
    @Mock private FileStorageOperationPersistencePort operationPersistencePort;
    @InjectMocks private FileCommandService commandService;

    private FileMetadata metadata;

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenant("inst-demo");
        TenantContextHolder.setTenantId(42L);
        metadata = FileMetadata.createWithId(FILE_ID, EntityType.TEXTBOOK, null,
                "notes.pdf", "application/pdf", 10L, OBJECT_KEY, null);
        metadata.markReady();
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void deleteFileCommitsTombstoneAndDurableIntentWithoutCallingMinio() {
        when(fileMetadataRepository.findById(FILE_ID)).thenReturn(Optional.of(metadata));
        commandService.deleteFile(FILE_ID);

        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.DELETING);
        assertThat(metadata.isUploadCompleted()).isFalse();
        ArgumentCaptor<FileStorageOperation> operation = ArgumentCaptor.forClass(FileStorageOperation.class);
        verify(operationPersistencePort).save(operation.capture());
        assertThat(operation.getValue().getFileId()).isEqualTo(FILE_ID);
        assertThat(operation.getValue().getOperationType()).isEqualTo(FileStorageOperationType.DELETE);
        assertThat(operation.getValue().getTargetObjectKey()).isEqualTo(OBJECT_KEY);
        verify(fileMetadataRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordDownloadIgnoresMissingIdsAndUsesTheTenantForValidDownloads() {
        commandService.recordDownload(null, 7L);
        commandService.recordDownload(FILE_ID, null);
        commandService.recordDownload(FILE_ID, 7L);

        verify(fileDownloadRepository).upsertDownload(42L, FILE_ID, 7L);
        verifyNoMoreInteractions(fileDownloadRepository);
    }

    @Test
    void deletingAnAlreadyDeletingFileDoesNotCreateDuplicateIntent() {
        metadata.markDeleting();
        when(fileMetadataRepository.findById(FILE_ID)).thenReturn(Optional.of(metadata));

        commandService.deleteFile(FILE_ID);

        verifyNoInteractions(operationPersistencePort);
    }
}
