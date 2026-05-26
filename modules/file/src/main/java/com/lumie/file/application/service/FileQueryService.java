package com.lumie.file.application.service;

import com.lumie.file.application.dto.request.PresignedDownloadRequest;
import com.lumie.file.application.dto.response.FileDownloadResult;
import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.dto.response.PresignedDownloadResponse;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileQueryService {

    private static final int DOWNLOAD_URL_EXPIRY_SECONDS = 3600; // 1 hour

    private final FileMetadataRepository fileMetadataRepository;
    private final StoragePort storagePort;

    @Transactional(readOnly = true)
    public FileDownloadResult downloadFile(UUID fileId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        if (!fileMetadata.isUploadCompleted()) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND, "File upload not completed");
        }

        InputStream stream = storagePort.getObject(fileMetadata.getObjectKey());
        log.debug("Streaming download for file: {}", fileId);

        return new FileDownloadResult(
                stream,
                fileMetadata.getOriginalFilename(),
                fileMetadata.getContentType(),
                fileMetadata.getFileSize()
        );
    }

    /**
     * Anonymous-accessible download for entity types that are intentionally
     * public (e.g. assets embedded in a published landing page). The caller
     * (controller) must guard which entity types are allowed here.
     */
    @Transactional(readOnly = true)
    public FileDownloadResult downloadPublicFile(UUID fileId, java.util.Set<EntityType> allowedTypes) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        if (!allowedTypes.contains(fileMetadata.getEntityType())) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
        if (!fileMetadata.isUploadCompleted()) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND, "File upload not completed");
        }

        InputStream stream = storagePort.getObject(fileMetadata.getObjectKey());
        return new FileDownloadResult(
                stream,
                fileMetadata.getOriginalFilename(),
                fileMetadata.getContentType(),
                fileMetadata.getFileSize()
        );
    }

    @Transactional(readOnly = true)
    public PresignedDownloadResponse generatePresignedDownloadUrl(PresignedDownloadRequest request) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(request.fileId())
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        if (!fileMetadata.isUploadCompleted()) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND, "File upload not completed");
        }

        String downloadUrl = storagePort.generatePresignedDownloadUrl(
                fileMetadata.getObjectKey(),
                DOWNLOAD_URL_EXPIRY_SECONDS
        );

        log.debug("Generated download URL for file: {}", request.fileId());

        return PresignedDownloadResponse.of(
                fileMetadata.getId(),
                downloadUrl,
                fileMetadata.getOriginalFilename(),
                fileMetadata.getContentType(),
                DOWNLOAD_URL_EXPIRY_SECONDS
        );
    }

    @Transactional(readOnly = true)
    public FileMetadataResponse getFileMetadata(UUID fileId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        return FileMetadataResponse.from(fileMetadata);
    }

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getFilesByEntity(EntityType entityType, Long entityId) {
        List<FileMetadata> files = fileMetadataRepository.findByEntityTypeAndEntityId(entityType, entityId);

        return files.stream()
                .map(FileMetadataResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getFilesByEntityAndFolder(EntityType entityType, UUID folderId) {
        List<FileMetadata> files = fileMetadataRepository.findByEntityTypeAndFolderId(entityType, folderId);

        return files.stream()
                .map(FileMetadataResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getAllFilesByEntityType(EntityType entityType) {
        List<FileMetadata> files = fileMetadataRepository.findByEntityType(entityType);

        return files.stream()
                .map(FileMetadataResponse::from)
                .toList();
    }
}
