package com.lumie.file.application.service;

import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.entity.TextbookFolder;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileStorageOperationType;
import com.lumie.file.domain.vo.FileUploadStatus;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.application.dto.request.CreateTextbookFolderRequest;
import com.lumie.file.application.dto.request.RenameTextbookFolderRequest;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TextbookFolderCommandServiceTest {

    @Mock private TextbookFolderRepository folderRepository;
    @Mock private FileMetadataRepository fileRepository;
    @Mock private StoragePort storagePort;
    @Mock private FileStorageOperationPersistencePort operationPersistencePort;
    @InjectMocks private TextbookFolderCommandService commandService;

    @Test
    void deletingParentRemovesNestedTextbookFilesAndFoldersInPostOrder() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID parentFileId = UUID.randomUUID();
        UUID childFileId = UUID.randomUUID();
        TextbookFolder parent = mock(TextbookFolder.class);
        TextbookFolder child = mock(TextbookFolder.class);
        FileMetadata parentFile = FileMetadata.createWithId(parentFileId, EntityType.TEXTBOOK, null,
                "parent.pdf", "application/pdf", 10L, "textbook/parent.pdf", parentId);
        FileMetadata childFile = FileMetadata.createWithId(childFileId, EntityType.TEXTBOOK, null,
                "child.pdf", "application/pdf", 20L, "textbook/child.pdf", childId);
        when(folderRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(folderRepository.findByParentId(parentId)).thenReturn(List.of(child));
        when(folderRepository.findByParentId(childId)).thenReturn(List.of());
        when(child.getId()).thenReturn(childId);
        when(fileRepository.findByEntityTypeAndFolderId(EntityType.TEXTBOOK, childId))
                .thenReturn(List.of(childFile));
        when(fileRepository.findByEntityTypeAndFolderId(EntityType.TEXTBOOK, parentId))
                .thenReturn(List.of(parentFile));

        parentFile.markReady();
        childFile.markReady();

        commandService.deleteFolder(parentId);

        assertThat(parentFile.getUploadStatus()).isEqualTo(FileUploadStatus.DELETING);
        assertThat(childFile.getUploadStatus()).isEqualTo(FileUploadStatus.DELETING);
        assertThat(parentFile.getFolderId()).isNull();
        assertThat(childFile.getFolderId()).isNull();
        ArgumentCaptor<FileStorageOperation> operations = ArgumentCaptor.forClass(FileStorageOperation.class);
        verify(operationPersistencePort, org.mockito.Mockito.times(2)).save(operations.capture());
        assertThat(operations.getAllValues()).extracting(FileStorageOperation::getOperationType)
                .containsOnly(FileStorageOperationType.DELETE);
        assertThat(operations.getAllValues()).extracting(FileStorageOperation::getFileId)
                .containsExactly(childFileId, parentFileId);
        org.mockito.InOrder deletionOrder = org.mockito.Mockito.inOrder(folderRepository);
        deletionOrder.verify(folderRepository).deleteById(childId);
        deletionOrder.verify(folderRepository).deleteById(parentId);
        verifyNoInteractions(storagePort);
    }

    @Test
    void createRootFolderAssignsDepthOne() {
        TextbookFolder folder = TextbookFolder.create("Math", null, 0);
        when(folderRepository.existsByParentIdAndName(null, "Math")).thenReturn(false);
        when(folderRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(folder);

        var response = commandService.createFolder(new CreateTextbookFolderRequest("Math", null));

        assertThat(response.name()).isEqualTo("Math");
        assertThat(response.depth()).isEqualTo(1);
    }

    @Test
    void createChildFolderUsesItsParentsDepth() {
        UUID parentId = UUID.randomUUID();
        TextbookFolder parent = TextbookFolder.create("Math", null, 0);
        when(folderRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(folderRepository.existsByParentIdAndName(parentId, "Algebra")).thenReturn(false);
        when(folderRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = commandService.createFolder(new CreateTextbookFolderRequest("Algebra", parentId));

        assertThat(response.parentId()).isEqualTo(parentId);
        assertThat(response.depth()).isEqualTo(2);
    }

    @Test
    void createFolderRejectsMissingParentAndDuplicateName() {
        UUID parentId = UUID.randomUUID();
        when(folderRepository.findById(parentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commandService.createFolder(new CreateTextbookFolderRequest("Child", parentId)))
                .isInstanceOfSatisfying(com.lumie.common.exception.BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FOLDER_NOT_FOUND));

        when(folderRepository.existsByParentIdAndName(null, "Math")).thenReturn(true);
        assertThatThrownBy(() -> commandService.createFolder(new CreateTextbookFolderRequest("Math", null)))
                .isInstanceOfSatisfying(com.lumie.common.exception.BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.DUPLICATE_FOLDER_NAME));
    }

    @Test
    void createFolderEnforcesMaximumDepth() {
        UUID parentId = UUID.randomUUID();
        TextbookFolder parent = TextbookFolder.create("Deep", null, 0);
        org.springframework.test.util.ReflectionTestUtils.setField(parent, "depth", 5);
        when(folderRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(folderRepository.existsByParentIdAndName(parentId, "Too deep")).thenReturn(false);

        assertThatThrownBy(() -> commandService.createFolder(new CreateTextbookFolderRequest("Too deep", parentId)))
                .isInstanceOfSatisfying(com.lumie.common.exception.BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.MAX_DEPTH_EXCEEDED));
    }

    @Test
    void renameSkipsDuplicateLookupWhenTheNameIsUnchanged() {
        UUID folderId = UUID.randomUUID();
        TextbookFolder folder = TextbookFolder.create("Math", null, 0);
        org.springframework.test.util.ReflectionTestUtils.setField(folder, "id", folderId);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        var response = commandService.renameFolder(folderId, new RenameTextbookFolderRequest("Math"));

        assertThat(response.name()).isEqualTo("Math");
        verify(folderRepository, never()).existsByParentIdAndName(null, "Math");
    }

    @Test
    void renameChecksDuplicatesAndRejectsMissingFolder() {
        UUID folderId = UUID.randomUUID();
        TextbookFolder folder = TextbookFolder.create("Math", null, 0);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(folderRepository.existsByParentIdAndName(null, "Science")).thenReturn(true);

        assertThatThrownBy(() -> commandService.renameFolder(folderId, new RenameTextbookFolderRequest("Science")))
                .isInstanceOfSatisfying(com.lumie.common.exception.BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.DUPLICATE_FOLDER_NAME));

        UUID missingId = UUID.randomUUID();
        when(folderRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commandService.renameFolder(missingId, new RenameTextbookFolderRequest("X")))
                .isInstanceOfSatisfying(com.lumie.common.exception.BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FOLDER_NOT_FOUND));
    }

    @Test
    void deleteFolderRejectsMissingFolder() {
        UUID folderId = UUID.randomUUID();
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.deleteFolder(folderId))
                .isInstanceOfSatisfying(com.lumie.common.exception.BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FOLDER_NOT_FOUND));
    }
}
