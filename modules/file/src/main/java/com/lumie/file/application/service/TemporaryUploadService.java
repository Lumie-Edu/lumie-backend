package com.lumie.file.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.dto.request.CreateTemporaryUploadRequest;
import com.lumie.file.application.dto.response.TemporaryUploadResponse;
import com.lumie.file.application.dto.response.TemporaryUploadStatusResponse;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FilePath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Owns the temporary browser-upload lifecycle for board and textbook files. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemporaryUploadService {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_ATTACHMENTS_PER_ENTITY = 5;
    private static final int PRESIGNED_URL_EXPIRY_SECONDS = 15 * 60;
    private static final Set<EntityType> SUPPORTED_TYPES = Set.of(
            EntityType.ANNOUNCEMENT, EntityType.QNA, EntityType.TEXTBOOK);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "hwp", "hwpx", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "jpg", "jpeg", "png", "webp", "gif", "zip");

    private final FileMetadataRepository fileMetadataRepository;
    private final StoragePort storagePort;

    @Transactional
    public TemporaryUploadResponse create(CreateTemporaryUploadRequest request) {
        validate(request);
        Long userId = requireCurrentUser();
        UUID fileId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        String objectKey = FilePath.of(request.entityType(), fileId, request.filename()).toTemporaryObjectKey();
        FileMetadata file = FileMetadata.createTemporary(
                request.entityType(), request.filename(), request.contentType(), request.fileSize(), objectKey, userId, expiresAt);
        fileMetadataRepository.save(file);
        String uploadUrl = storagePort.generatePresignedUploadUrl(objectKey, request.contentType(), PRESIGNED_URL_EXPIRY_SECONDS);
        return new TemporaryUploadResponse(file.getId(), uploadUrl, expiresAt);
    }

    @Transactional
    public TemporaryUploadStatusResponse complete(UUID fileId) {
        FileMetadata file = requireOwnedTemporary(fileId);
        if (file.isUploadCompleted()) {
            return TemporaryUploadStatusResponse.from(file);
        }
        if (!storagePort.objectExists(file.getObjectKey()) || storagePort.objectSize(file.getObjectKey()) != file.getFileSize()) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "Uploaded object does not match the requested file size");
        }
        file.markUploadCompleted(storagePort.objectEtag(file.getObjectKey()));
        return TemporaryUploadStatusResponse.from(file);
    }

    @Transactional(readOnly = true)
    public TemporaryUploadStatusResponse status(UUID fileId) {
        return TemporaryUploadStatusResponse.from(requireOwnedTemporary(fileId));
    }

    @Transactional
    public void cancel(UUID fileId) {
        FileMetadata file = requireOwnedTemporary(fileId);
        file.markDeleting();
        deleteObjects(file);
        fileMetadataRepository.delete(file);
    }

    /**
     * Copies uploaded tmp objects to durable paths. A caller invokes this before
     * opening its domain write transaction, then calls {@link #attachPrepared} inside it.
     */
    public void prepareForAttachment(EntityType entityType, List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return;
        requireSupportedType(entityType);
        for (UUID fileId : fileIds.stream().distinct().toList()) {
            FileMetadata file = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
            if (!file.isTemporary()) continue;
            file = requireOwnedTemporary(fileId);
            if (file.getEntityType() != entityType || !file.isUploadCompleted()) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "Only completed uploads of the requested type can be attached");
            }
            if (file.getLifecycleStatus() == com.lumie.file.domain.vo.FileLifecycleStatus.READY_TO_ATTACH) continue;
            String destination = FilePath.of(entityType, file.getId(), file.getOriginalFilename()).toObjectKey();
            storagePort.copyObject(file.getObjectKey(), destination, file.getCompletedEtag());
            if (storagePort.objectSize(destination) != file.getFileSize()) {
                storagePort.deleteObject(destination);
                throw new BusinessException(FileErrorCode.STORAGE_ERROR, "Copied object size did not match upload");
            }
            file.markPrepared(destination);
            fileMetadataRepository.save(file);
        }
    }

    @Transactional
    public void attachPrepared(EntityType entityType, Long entityId, List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return;
        requireSupportedType(entityType);
        int existing = fileMetadataRepository.findByEntityTypeAndEntityId(entityType, entityId).size();
        if (existing + fileIds.stream().distinct().count() > MAX_ATTACHMENTS_PER_ENTITY) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "A post may have at most five attachments");
        }
        List<String> temporaryKeys = new java.util.ArrayList<>();
        for (UUID fileId : fileIds.stream().distinct().toList()) {
            FileMetadata file = requireOwnedTemporary(fileId);
            if (file.getEntityType() != entityType || !file.isUploadCompleted()
                    || file.getLifecycleStatus() != com.lumie.file.domain.vo.FileLifecycleStatus.READY_TO_ATTACH) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_DATA, "Temporary upload is not ready to attach");
            }
            temporaryKeys.add(file.getTemporaryObjectKey());
            file.linkToEntity(entityId);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                temporaryKeys.stream().filter(java.util.Objects::nonNull).forEach(key -> {
                    try {
                        storagePort.deleteObject(key);
                    } catch (BusinessException exception) {
                        log.warn("Temporary object cleanup after attachment failed: {}", key, exception);
                    }
                });
            }
        });
    }

    @Transactional
    public void cleanupExpired() {
        List<FileMetadata> expired = fileMetadataRepository.findByLifecycleStatusInAndExpiresAtBefore(
                List.of(com.lumie.file.domain.vo.FileLifecycleStatus.TEMPORARY,
                        com.lumie.file.domain.vo.FileLifecycleStatus.READY_TO_ATTACH), Instant.now());
        for (FileMetadata file : expired) {
            file.markDeleting();
            deleteObjects(file);
            fileMetadataRepository.delete(file);
        }
        if (!expired.isEmpty()) log.info("Cleaned up {} expired temporary upload(s)", expired.size());
    }

    private FileMetadata requireOwnedTemporary(UUID fileId) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        if (!file.isTemporaryOwnedBy(requireCurrentUser())) {
            throw new BusinessException(FileErrorCode.ACCESS_DENIED);
        }
        return file;
    }

    private static void validate(CreateTemporaryUploadRequest request) {
        requireSupportedType(request.entityType());
        if (request.fileSize() > MAX_FILE_SIZE_BYTES || !hasAllowedExtension(request.filename())
                || request.filename().contains("/") || request.filename().contains("\\") || request.filename().contains("..")) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
        }
    }

    private static void requireSupportedType(EntityType entityType) {
        if (!SUPPORTED_TYPES.contains(entityType)) {
            throw new BusinessException(FileErrorCode.INVALID_ENTITY_TYPE);
        }
    }

    private static boolean hasAllowedExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 && ALLOWED_EXTENSIONS.contains(filename.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static Long requireCurrentUser() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) throw new BusinessException(FileErrorCode.ACCESS_DENIED);
        return userId;
    }

    private void deleteObjects(FileMetadata file) {
        try {
            storagePort.deleteObject(file.getObjectKey());
            if (file.getTemporaryObjectKey() != null) storagePort.deleteObject(file.getTemporaryObjectKey());
        } catch (BusinessException exception) {
            log.warn("Temporary object cleanup failed for file {}", file.getId(), exception);
        }
    }
}
