package com.lumie.file.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.repository.FileDownloadRepository;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileStorageOperationType;
import com.lumie.file.domain.vo.FileUploadStatus;
import com.lumie.file.application.dto.request.RegisterTextbookFilesRequest;
import com.lumie.file.api.PreparedTemporaryFile;
import com.lumie.file.api.FileService;
import java.util.Optional;
import java.util.List;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class FileCommandServiceTest {

    private static final UUID FILE_ID = UUID.fromString("d14b9d11-eaab-4f37-bac5-3d4d205e65d0");
    private static final String OBJECT_KEY = "files/demo/textbook/file-1/notes.pdf";

    @Mock private FileMetadataRepository fileMetadataRepository;
    @Mock private TextbookFolderRepository textbookFolderRepository;
    @Mock private FileDownloadRepository fileDownloadRepository;
    @Mock private FileService fileService;
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
        verify(fileMetadataRepository, never()).deleteById(FILE_ID);
    }

    @Test
    void registerTextbookFilesValidatesFolderAndReturnsAttachedMetadata() {
        when(fileService.prepareTemporaryFiles("textbook", List.of())).thenReturn(List.of());
        when(fileService.attachPreparedFiles("textbook", null, List.of(), null)).thenReturn(List.of(FILE_ID));
        when(fileMetadataRepository.findById(FILE_ID)).thenReturn(Optional.of(metadata));

        var response = commandService.registerTextbookFiles(new RegisterTextbookFilesRequest(null, List.of()));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(FILE_ID);
        verify(fileMetadataRepository).flush();
    }

    @Test
    void registerTextbookFilesRejectsUnknownFolderBeforePreparingUploads() {
        UUID folderId = UUID.randomUUID();
        when(textbookFolderRepository.findById(folderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.registerTextbookFiles(
                new RegisterTextbookFilesRequest(folderId, List.of())))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        verifyNoInteractions(fileService);
    }

    @Test
    void registerTextbookFilesDiscardsPreparedDescriptorsWhenMetadataCannotBeReloaded() {
        PreparedTemporaryFile prepared = new PreparedTemporaryFile(
                FILE_ID, "notes.pdf", "application/pdf", 10L, OBJECT_KEY);
        when(fileService.prepareTemporaryFiles("textbook", List.of())).thenReturn(List.of(prepared));
        when(fileService.attachPreparedFiles("textbook", null, List.of(prepared), null)).thenReturn(List.of(FILE_ID));
        when(fileMetadataRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.registerTextbookFiles(new RegisterTextbookFilesRequest(null, List.of())))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        verify(fileService).discardPreparedFiles(List.of(prepared));
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
