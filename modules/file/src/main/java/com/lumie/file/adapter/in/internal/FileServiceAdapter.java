package com.lumie.file.adapter.in.internal;

import com.lumie.file.api.FileObjectKeyFactory;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.application.service.TemporaryObjectUploadService;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.file.api.PreparedTemporaryFile;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.api.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileServiceAdapter implements FileService {

    private static final int MAX_ATTACHMENTS = 5;
    private static final Duration TEMPORARY_SOURCE_SAFETY_WINDOW = Duration.ofHours(22);

    private final FileMetadataRepository fileMetadataRepository;
    private final StoragePort storagePort;
    private final FileStorageOperationPersistencePort operationPersistencePort;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<PreparedTemporaryFile> prepareTemporaryFiles(String entityType, List<TemporaryAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return List.of();
        EntityType type = parseEntityType(entityType);
        validateNewAttachments(attachments);
        List<PreparedTemporaryFile> prepared = new ArrayList<>();
        try {
            for (TemporaryAttachment attachment : attachments) {
                TemporarySourceInfo source = validateTemporaryObject(attachment);
                UUID id = UUID.randomUUID();
                String durable = com.lumie.file.domain.vo.FilePath.of(type, id, attachment.filename()).toObjectKey();
                PreparedTemporaryFile preparedFile = new PreparedTemporaryFile(id, attachment.filename(),
                        attachment.contentType(), attachment.fileSize(), source.objectKey(), durable,
                        source.etag(), source.lastModified());
                prepared.add(preparedFile);
            }
            return List.copyOf(prepared);
        } catch (RuntimeException exception) {
            discardPreparedFiles(prepared);
            throw exception;
        }
    }

    @Override
    @Transactional
    public List<UUID> attachPreparedFiles(String entityType, Long entityId, List<PreparedTemporaryFile> files) {
        return attachPreparedFiles(entityType, entityId, files, null);
    }

    @Override
    @Transactional
    public List<UUID> attachPreparedFiles(String entityType, Long entityId, List<PreparedTemporaryFile> files, UUID folderId) {
        if (files == null || files.isEmpty()) return List.of();
        EntityType type = parseEntityType(entityType);
        List<UUID> ids = new ArrayList<>();
        for (PreparedTemporaryFile prepared : files) {
            FileMetadata file = FileMetadata.create(type, entityId,
                    prepared.filename(), prepared.contentType(), prepared.fileSize(), prepared.durableObjectKey(), folderId);
            FileMetadata saved = fileMetadataRepository.save(file);
            operationPersistencePort.save(FileStorageOperation.promote(saved.getId(),
                    prepared.temporaryObjectKey(), prepared.durableObjectKey(), prepared.sourceEtag(),
                    prepared.fileSize(), prepared.contentType(), prepared.sourceLastModified()));
            ids.add(saved.getId());
        }
        return ids;
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
    @Transactional
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
    @Transactional
    public void deleteFilesByEntity(String entityType, Long entityId) {
        EntityType type = parseEntityType(entityType);

        List<FileMetadata> files = fileMetadataRepository.findByEntityTypeAndEntityId(type, entityId);
        if (files.isEmpty()) {
            return;
        }

        for (FileMetadata file : files) {
            enqueueDelete(file);
        }
        log.info("Enqueued file deletion for entity={}/{} count={}", entityType, entityId,
                files.stream().filter(file -> file.isDeleting()).count());
    }

    @Override
    @Transactional
    public void deleteFilesByIds(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        for (UUID id : fileIds) {
            fileMetadataRepository.findById(id).ifPresent(file -> {
                if (file.getEntityType() == EntityType.TEXTBOOK) {
                    throw new BusinessException(FileErrorCode.FILE_IN_USE, "Textbook files must be unlinked, not deleted from posts");
                }
                enqueueDelete(file);
            });
        }
    }

    @Override
    @Transactional
    public void deleteFilesByEntityAndIds(String entityType, Long entityId, List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return;
        EntityType type = parseEntityType(entityType);
        for (UUID fileId : fileIds.stream().distinct().toList()) {
            fileMetadataRepository.findById(fileId)
                    .filter(file -> file.getEntityType() == type && entityId.equals(file.getEntityId()))
                    .ifPresent(this::enqueueDelete);
        }
    }

    private void enqueueDelete(FileMetadata file) {
        if (file.isDeleting()) return;
        file.markDeleting();
        operationPersistencePort.save(FileStorageOperation.delete(
                file.getId(), file.getObjectKey(), Instant.now()));
    }

    private static EntityType parseEntityType(String entityType) {
        return Arrays.stream(EntityType.values())
                .filter(e -> e.getPathSegment().equals(entityType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + entityType));
    }

    private void validateNewAttachments(List<TemporaryAttachment> attachments) {
        if (attachments == null) return;
        if (attachments.size() > MAX_ATTACHMENTS || attachments.stream().anyMatch(Objects::isNull)
                || attachments.stream().map(TemporaryAttachment::objectKey).distinct().count() != attachments.size()) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "Invalid attachment list");
        }
        for (TemporaryAttachment attachment : attachments) {
            if (attachment == null || attachment.fileSize() <= 0 || attachment.fileSize() > TemporaryObjectUploadService.MAX_BYTES
                    || attachment.contentType() == null || attachment.contentType().isBlank()
                    || attachment.filename() == null
                    || !TemporaryObjectUploadService.isAllowedFilename(attachment.filename())) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
            }
        }
    }

    private TemporarySourceInfo validateTemporaryObject(TemporaryAttachment attachment) {
        String tenantId = com.lumie.common.tenant.TenantContextHolder.toStorageTenantId(
                com.lumie.common.tenant.TenantContextHolder.getRequiredTenant());
        if (!FileObjectKeyFactory.isTemporaryUploadKey(tenantId, attachment.objectKey(), attachment.filename())
                || !storagePort.objectExists(attachment.objectKey())) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
        }
        long actualSize = storagePort.objectSize(attachment.objectKey());
        String actualContentType = storagePort.objectContentType(attachment.objectKey());
        String etag = storagePort.objectEtag(attachment.objectKey());
        Instant lastModified = storagePort.objectLastModified(attachment.objectKey());
        Instant now = Instant.now();
        if (actualSize != attachment.fileSize()
                || actualContentType == null || !actualContentType.equalsIgnoreCase(attachment.contentType())
                || etag == null || etag.isBlank() || lastModified == null
                || lastModified.isBefore(now.minus(TEMPORARY_SOURCE_SAFETY_WINDOW))
                || lastModified.isAfter(now.plus(Duration.ofMinutes(5)))) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
        }
        return new TemporarySourceInfo(attachment.objectKey(), etag, lastModified);
    }

    private record TemporarySourceInfo(String objectKey, String etag, Instant lastModified) { }

}
