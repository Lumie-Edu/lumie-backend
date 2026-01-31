package com.lumie.file.application.service;

import com.lumie.file.application.dto.request.PresignedUploadRequest;
import com.lumie.file.application.dto.request.RegisterUploadRequest;
import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.dto.response.PresignedUploadResponse;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.exception.FileException;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FilePath;
import com.lumie.file.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCommandService {

    private static final int UPLOAD_URL_EXPIRY_SECONDS = 3600; // 1 hour

    private final FileMetadataRepository fileMetadataRepository;
    private final StoragePort storagePort;

    @Transactional
    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) {
        String tenantSlug = getTenantSlugForEntityType(request.entityType());
        UUID fileId = UUID.randomUUID();

        FilePath filePath = FilePath.of(
                tenantSlug,
                request.entityType(),
                fileId,
                request.filename()
        );

        String objectKey = filePath.toObjectKey();

        FileMetadata fileMetadata = FileMetadata.create(
                tenantSlug,
                request.entityType(),
                request.entityId(),
                request.filename(),
                request.contentType(),
                request.fileSize(),
                objectKey
        );

        FileMetadata saved = fileMetadataRepository.save(fileMetadata);
        log.info("Created file metadata: {} for tenant: {}", saved.getId(), tenantSlug);

        String uploadUrl = storagePort.generatePresignedUploadUrl(
                objectKey,
                request.contentType(),
                UPLOAD_URL_EXPIRY_SECONDS
        );

        return PresignedUploadResponse.of(saved.getId(), uploadUrl, UPLOAD_URL_EXPIRY_SECONDS);
    }

    @Transactional
    public FileMetadataResponse registerUploadCompleted(RegisterUploadRequest request) {
        String tenantSlug = TenantContextHolder.getTenant();

        FileMetadata fileMetadata = findFileMetadata(request.fileId(), tenantSlug);

        if (!storagePort.objectExists(fileMetadata.getObjectKey())) {
            log.warn("Upload not found in storage for file: {}", request.fileId());
            throw new FileException(FileErrorCode.FILE_NOT_FOUND, "File not uploaded to storage");
        }

        fileMetadata.markUploadCompleted();
        log.info("Marked file upload as completed: {}", request.fileId());

        return FileMetadataResponse.from(fileMetadata);
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        String tenantSlug = TenantContextHolder.getTenant();

        FileMetadata fileMetadata = findFileMetadata(fileId, tenantSlug);

        storagePort.deleteObject(fileMetadata.getObjectKey());
        fileMetadataRepository.deleteById(fileId);

        log.info("Deleted file: {} for tenant: {}", fileId, tenantSlug);
    }

    private FileMetadata findFileMetadata(UUID fileId, String tenantSlug) {
        if (tenantSlug == null) {
            return fileMetadataRepository.findById(fileId)
                    .filter(FileMetadata::isPlatformFile)
                    .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));
        }

        return fileMetadataRepository.findByIdAndTenantSlug(fileId, tenantSlug)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));
    }

    private String getTenantSlugForEntityType(EntityType entityType) {
        if (entityType == EntityType.LOGO) {
            return null;
        }
        return TenantContextHolder.getRequiredTenant();
    }
}
