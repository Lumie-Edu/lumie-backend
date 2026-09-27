package com.lumie.file.adapter.in.internal;

import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.application.service.TemporaryObjectUploadService;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.file.api.PreparedTemporaryFile;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

    private static final int MAX_ATTACHMENTS = 5;

    private final FileMetadataRepository fileMetadataRepository;
    private final FileLinkRepository fileLinkRepository;
    private final StoragePort storagePort;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<PreparedTemporaryFile> prepareTemporaryFiles(String entityType, List<TemporaryAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return List.of();
        EntityType type = parseEntityType(entityType);
        validateNewAttachments(attachments);
        List<PreparedTemporaryFile> prepared = new ArrayList<>();
        try {
            for (TemporaryAttachment attachment : attachments) {
                validateTemporaryObject(attachment);
                UUID id = UUID.randomUUID();
                String durable = com.lumie.file.domain.vo.FilePath.of(type, id, attachment.filename()).toObjectKey();
                storagePort.copyObject(attachment.objectKey(), durable, storagePort.objectEtag(attachment.objectKey()));
                prepared.add(new PreparedTemporaryFile(id, attachment.filename(), attachment.contentType(), attachment.fileSize(), durable));
            }
            return List.copyOf(prepared);
        } catch (RuntimeException exception) {
            discardPreparedFiles(prepared);
            throw exception;
        }
    }

    @Override
    public void attachPreparedFiles(String entityType, Long entityId, List<PreparedTemporaryFile> files) {
        if (files == null || files.isEmpty()) return;
        EntityType type = parseEntityType(entityType);
        for (PreparedTemporaryFile prepared : files) {
            FileMetadata file = FileMetadata.createWithId(prepared.fileId(), type, entityId,
                    prepared.filename(), prepared.contentType(), prepared.fileSize(), prepared.durableObjectKey(), null);
            file.markUploadCompleted();
            fileMetadataRepository.save(file);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void discardPreparedFiles(List<PreparedTemporaryFile> files) {
        if (files == null) return;
        for (PreparedTemporaryFile prepared : files) {
            try {
                storagePort.deleteObject(prepared.durableObjectKey());
            } catch (RuntimeException exception) {
                log.warn("Failed to compensate prepared object: {}", prepared.durableObjectKey(), exception);
            }
        }
    }
    @Override
    public void reconcilePreparedDirectFiles(String entityType, Long entityId, List<UUID> retainedFileIds,
                                              List<PreparedTemporaryFile> preparedFiles) {
        if (retainedFileIds == null && (preparedFiles == null || preparedFiles.isEmpty())) return;
        EntityType type = parseEntityType(entityType);
        List<UUID> requested = retainedFileIds == null ? List.of() : retainedFileIds.stream().distinct().toList();
        List<FileMetadata> existing = fileMetadataRepository.findByEntityTypeAndEntityId(type, entityId);
        List<UUID> existingIds = existing.stream().map(FileMetadata::getId).toList();
        if (!existingIds.containsAll(requested)) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "Retained attachments must belong to this post");
        }
        int preparedCount = preparedFiles == null ? 0 : preparedFiles.size();
        if (requested.size() + preparedCount > MAX_ATTACHMENTS) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "A post can have at most five attachments");
        }
        deleteFilesByEntityAndIds(entityType, entityId, existingIds.stream().filter(id -> !requested.contains(id)).toList());
        attachPreparedFiles(entityType, entityId, preparedFiles);
    }

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
    public void deleteFilesByEntityAndIds(String entityType, Long entityId, List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return;
        EntityType type = parseEntityType(entityType);
        List<String> objectKeys = new ArrayList<>();
        for (UUID fileId : fileIds.stream().distinct().toList()) {
            fileMetadataRepository.findById(fileId)
                    .filter(file -> file.getEntityType() == type && entityId.equals(file.getEntityId()))
                    .ifPresent(file -> {
                        objectKeys.add(file.getObjectKey());
                        fileMetadataRepository.delete(file);
                    });
        }
        if (!objectKeys.isEmpty()) scheduleMinioCleanup(objectKeys);
    }

    @Override
    public void replaceFileLinks(String entityType, Long entityId, List<UUID> fileIds) {
        if (fileIds == null) {
            return;
        }

        EntityType type = parseEntityType(entityType);
        fileLinkRepository.deleteByEntityTypeAndEntityId(type, entityId);
        fileLinkRepository.flush();

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

    private void validateNewAttachments(List<TemporaryAttachment> attachments) {
        if (attachments == null) return;
        if (attachments.size() > MAX_ATTACHMENTS
                || attachments.stream().map(TemporaryAttachment::objectKey).distinct().count() != attachments.size()) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "Invalid attachment list");
        }
        for (TemporaryAttachment attachment : attachments) {
            if (attachment == null || attachment.fileSize() <= 0 || attachment.fileSize() > TemporaryObjectUploadService.MAX_BYTES
                    || attachment.contentType() == null || attachment.contentType().isBlank()
                    || !TemporaryObjectUploadService.isAllowedFilename(attachment.filename())) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
            }
        }
    }

    private void validateTemporaryObject(TemporaryAttachment attachment) {
        String tenantId = com.lumie.common.tenant.TenantContextHolder.toStorageTenantId(
                com.lumie.common.tenant.TenantContextHolder.getRequiredTenant());
        String prefix = "tmp/" + tenantId + "/uploads/";
        if (attachment.objectKey() == null || !attachment.objectKey().startsWith(prefix)
                || !storagePort.objectExists(attachment.objectKey())
                || storagePort.objectSize(attachment.objectKey()) != attachment.fileSize()) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
        }
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
