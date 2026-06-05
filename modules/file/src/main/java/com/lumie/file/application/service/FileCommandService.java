package com.lumie.file.application.service;

import com.lumie.file.application.dto.request.PresignedUploadRequest;
import com.lumie.file.application.dto.request.RegisterUploadRequest;
import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.dto.response.PresignedUploadResponse;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.repository.FileLinkRepository;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FilePath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCommandService {

    private static final int UPLOAD_URL_EXPIRY_SECONDS = 3600; // 1 hour

    private final FileMetadataRepository fileMetadataRepository;
    private final FileLinkRepository fileLinkRepository;
    private final TextbookFolderRepository textbookFolderRepository;
    private final StoragePort storagePort;

    @Transactional
    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) {
        UUID fileId = UUID.randomUUID();

        FilePath filePath = FilePath.of(
                request.entityType(),
                fileId,
                request.filename()
        );

        String objectKey = filePath.toObjectKey();

        FileMetadata fileMetadata = FileMetadata.create(
                request.entityType(),
                request.entityId(),
                request.filename(),
                request.contentType(),
                request.fileSize(),
                objectKey,
                null
        );

        FileMetadata saved = fileMetadataRepository.save(fileMetadata);
        log.info("Created file metadata: {}", saved.getId());

        String uploadUrl = storagePort.generatePresignedUploadUrl(
                objectKey,
                request.contentType(),
                UPLOAD_URL_EXPIRY_SECONDS
        );

        return PresignedUploadResponse.of(saved.getId(), uploadUrl, UPLOAD_URL_EXPIRY_SECONDS);
    }

    @Transactional
    public FileMetadataResponse uploadFile(EntityType entityType, Long entityId, UUID folderId, MultipartFile file) {
        if (folderId != null) {
            textbookFolderRepository.findById(folderId)
                    .orElseThrow(() -> new BusinessException(FileErrorCode.FOLDER_NOT_FOUND));
        }

        UUID fileId = UUID.randomUUID();

        FilePath filePath = FilePath.of(entityType, fileId, file.getOriginalFilename());
        String objectKey = filePath.toObjectKey();

        FileMetadata fileMetadata = FileMetadata.create(
                entityType, entityId, file.getOriginalFilename(),
                file.getContentType(), file.getSize(), objectKey, folderId
        );

        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        try {
            storagePort.uploadObject(objectKey, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("Failed to read upload stream for file: {}", saved.getId(), e);
            throw new BusinessException(FileErrorCode.STORAGE_ERROR);
        }

        saved.markUploadCompleted();
        log.info("Uploaded file: {}", saved.getId());

        return FileMetadataResponse.from(saved);
    }

    @Transactional
    public FileMetadataResponse registerUploadCompleted(RegisterUploadRequest request) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(request.fileId())
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        if (!storagePort.objectExists(fileMetadata.getObjectKey())) {
            log.warn("Upload not found in storage for file: {}", request.fileId());
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND, "File not uploaded to storage");
        }

        fileMetadata.markUploadCompleted();
        log.info("Marked file upload as completed: {}", request.fileId());

        return FileMetadataResponse.from(fileMetadata);
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        if (fileLinkRepository.existsByFile_Id(fileId)) {
            throw new BusinessException(FileErrorCode.FILE_IN_USE);
        }

        storagePort.deleteObject(fileMetadata.getObjectKey());
        fileMetadataRepository.deleteById(fileId);

        log.info("Deleted file: {}", fileId);
    }
}
