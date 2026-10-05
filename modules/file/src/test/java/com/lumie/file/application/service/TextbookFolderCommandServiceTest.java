package com.lumie.file.application.service;

import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.TextbookFolder;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import com.lumie.file.domain.vo.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextbookFolderCommandServiceTest {

    @Mock private TextbookFolderRepository folderRepository;
    @Mock private FileMetadataRepository fileRepository;
    @Mock private StoragePort storagePort;
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

        commandService.deleteFolder(parentId);

        InOrder deletionOrder = inOrder(storagePort, fileRepository, folderRepository);
        deletionOrder.verify(storagePort).deleteObject("textbook/child.pdf");
        deletionOrder.verify(fileRepository).deleteById(childFileId);
        deletionOrder.verify(folderRepository).deleteById(childId);
        deletionOrder.verify(storagePort).deleteObject("textbook/parent.pdf");
        deletionOrder.verify(fileRepository).deleteById(parentFileId);
        deletionOrder.verify(folderRepository).deleteById(parentId);
    }
}
