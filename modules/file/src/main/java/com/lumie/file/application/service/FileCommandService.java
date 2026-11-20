package com.lumie.file.application.service;

import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.port.out.FileMetadataPersistencePort;
import com.lumie.file.domain.entity.FileStorageOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCommandService {

    private final FileMetadataPersistencePort fileMetadataRepository;
    private final com.lumie.file.application.port.out.FileDownloadPersistencePort fileDownloadRepository;
    private final FileStorageOperationPersistencePort operationPersistencePort;

    @Transactional
    public void recordDownload(UUID fileId, Long userId) {
        if (fileId == null || userId == null) return;

        fileDownloadRepository.upsertDownload(TenantContextHolder.getRequiredTenantId(), fileId, userId);
    }

    private void enqueueDelete(FileMetadata fileMetadata) {
        if (fileMetadata.isDeleting()) return;
        fileMetadata.markDeleting();
        operationPersistencePort.save(FileStorageOperation.delete(
                fileMetadata.getId(), fileMetadata.getObjectKey(), java.time.Instant.now()));
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        // Keep archived textbook metadata deletable without violating the legacy folder FK.
        if (fileMetadata.getFolderId() != null) {
            fileMetadata.clearFolder();
        }
        enqueueDelete(fileMetadata);

        log.info("Deleted file: {}", fileId);
    }
}
