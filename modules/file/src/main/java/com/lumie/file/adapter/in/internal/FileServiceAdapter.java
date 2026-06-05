package com.lumie.file.adapter.in.internal;

import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileLink;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.domain.repository.FileLinkRepository;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.api.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileServiceAdapter implements FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileLinkRepository fileLinkRepository;
    private final StoragePort storagePort;

    @Override
    public void deleteFilesByEntity(String entityType, Long entityId) {
        EntityType type = parseEntityType(entityType);

        List<FileMetadata> files = fileMetadataRepository.findByEntityTypeAndEntityId(type, entityId);
        if (files.isEmpty()) {
            return;
        }

        List<String> objectKeys = new ArrayList<>();
        for (FileMetadata file : files) {
            objectKeys.add(file.getObjectKey());
            fileMetadataRepository.deleteById(file.getId());
        }

        log.info("Marked {} file(s) for MinIO deletion (entity={}/{})", objectKeys.size(), entityType, entityId);
        scheduleMinioCleanup(objectKeys);
    }

    @Override
    public void deleteFilesByIds(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        List<String> objectKeys = new ArrayList<>();
        for (UUID id : fileIds) {
            fileMetadataRepository.findById(id).ifPresent(file -> {
                if (file.getEntityType() == EntityType.TEXTBOOK) {
                    throw new BusinessException(FileErrorCode.FILE_IN_USE, "Textbook files must be unlinked, not deleted from posts");
                }
                objectKeys.add(file.getObjectKey());
                fileMetadataRepository.deleteById(file.getId());
            });
        }

        if (objectKeys.isEmpty()) {
            return;
        }

        log.info("Marked {} file(s) for MinIO deletion by ID", objectKeys.size());
        scheduleMinioCleanup(objectKeys);
    }

    @Override
    public void replaceFileLinks(String entityType, Long entityId, List<UUID> fileIds) {
        if (fileIds == null) {
            return;
        }

        EntityType type = parseEntityType(entityType);
        fileLinkRepository.deleteByEntityTypeAndEntityId(type, entityId);

        if (fileIds.isEmpty()) {
            return;
        }

        for (UUID fileId : fileIds.stream().distinct().toList()) {
            FileMetadata file = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
            if (file.getEntityType() != EntityType.TEXTBOOK || !file.isUploadCompleted()) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "Only completed textbook files can be linked");
            }
            fileLinkRepository.save(FileLink.create(file, type, entityId));
        }
    }

    @Override
    public void deleteFileLinksByEntity(String entityType, Long entityId) {
        fileLinkRepository.deleteByEntityTypeAndEntityId(parseEntityType(entityType), entityId);
    }

    private static EntityType parseEntityType(String entityType) {
        return Arrays.stream(EntityType.values())
                .filter(e -> e.getPathSegment().equals(entityType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + entityType));
    }

    /**
     * MinIO 삭제는 DB 트랜잭션 커밋 이후에 실행.
     * 트랜잭션이 롤백되면 MinIO 객체는 삭제되지 않아 일관성 유지.
     */
    private void scheduleMinioCleanup(List<String> objectKeys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String key : objectKeys) {
                    try {
                        storagePort.deleteObject(key);
                        log.debug("Deleted MinIO object after commit: {}", key);
                    } catch (Exception e) {
                        log.error("Failed to delete MinIO object after commit: {}", key, e);
                    }
                }
            }
        });
    }
}
