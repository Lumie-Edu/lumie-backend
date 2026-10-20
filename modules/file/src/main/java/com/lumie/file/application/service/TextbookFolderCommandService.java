package com.lumie.file.application.service;

import com.lumie.file.application.dto.request.CreateTextbookFolderRequest;
import com.lumie.file.application.dto.request.RenameTextbookFolderRequest;
import com.lumie.file.application.dto.response.TextbookFolderResponse;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.entity.TextbookFolder;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import com.lumie.file.domain.vo.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextbookFolderCommandService {

    private final TextbookFolderRepository textbookFolderRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageOperationPersistencePort operationPersistencePort;

    @Transactional
    public TextbookFolderResponse createFolder(CreateTextbookFolderRequest request) {
        int parentDepth = 0;

        if (request.parentId() != null) {
            TextbookFolder parent = textbookFolderRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(FileErrorCode.FOLDER_NOT_FOUND));
            parentDepth = parent.getDepth();
        }

        if (textbookFolderRepository.existsByParentIdAndName(request.parentId(), request.name())) {
            throw new BusinessException(FileErrorCode.DUPLICATE_FOLDER_NAME);
        }

        TextbookFolder folder = TextbookFolder.create(request.name(), request.parentId(), parentDepth);
        TextbookFolder saved = textbookFolderRepository.save(folder);

        log.info("Created textbook folder: {} (depth: {})", saved.getId(), saved.getDepth());
        return TextbookFolderResponse.from(saved);
    }

    @Transactional
    public TextbookFolderResponse renameFolder(UUID folderId, RenameTextbookFolderRequest request) {
        TextbookFolder folder = textbookFolderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getName().equals(request.name())
                && textbookFolderRepository.existsByParentIdAndName(folder.getParentId(), request.name())) {
            throw new BusinessException(FileErrorCode.DUPLICATE_FOLDER_NAME);
        }

        folder.rename(request.name());
        log.info("Renamed textbook folder: {}", folderId);
        return TextbookFolderResponse.from(folder);
    }

    @Transactional
    public void deleteFolder(UUID folderId) {
        if (textbookFolderRepository.findById(folderId).isEmpty()) {
            throw new BusinessException(FileErrorCode.FOLDER_NOT_FOUND);
        }

        deleteFolderRecursive(folderId);
        log.info("Deleted textbook folder (cascade): {}", folderId);
    }

    private void deleteFolderRecursive(UUID folderId) {
        // 1. 자식 폴더 먼저 재귀 삭제 (post-order: 자식 → 부모 순)
        List<TextbookFolder> children = textbookFolderRepository.findByParentId(folderId);
        for (TextbookFolder child : children) {
            deleteFolderRecursive(child.getId());
        }

        // 2. 이 폴더의 파일들을 S3 + DB에서 삭제
        List<FileMetadata> files = fileMetadataRepository.findByEntityTypeAndFolderId(EntityType.TEXTBOOK, folderId);
        for (FileMetadata file : files) {
            if (!file.isDeleting()) {
                file.markDeleting();
                file.clearFolder();
                operationPersistencePort.save(FileStorageOperation.delete(
                        file.getId(), file.getObjectKey(), java.time.Instant.now()));
                log.debug("Enqueued textbook object deletion from folder {}: {}", folderId, file.getId());
            }
        }

        // 3. 폴더 자체 삭제
        textbookFolderRepository.deleteById(folderId);
    }
}
