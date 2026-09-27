package com.lumie.file.application.service;

import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.dto.request.RegisterTextbookFilesRequest;
import com.lumie.file.api.FileService;
import com.lumie.file.api.PreparedTemporaryFile;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.repository.FileLinkRepository;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import com.lumie.file.domain.vo.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCommandService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileLinkRepository fileLinkRepository;
    private final TextbookFolderRepository textbookFolderRepository;
    private final com.lumie.file.domain.repository.FileDownloadRepository fileDownloadRepository;
    private final StoragePort storagePort;
    private final FileService fileService;

    @Transactional
    public List<FileMetadataResponse> registerTextbookFiles(RegisterTextbookFilesRequest request) {
        if (request.folderId() != null && textbookFolderRepository.findById(request.folderId()).isEmpty()) {
            throw new BusinessException(FileErrorCode.FOLDER_NOT_FOUND);
        }
        List<PreparedTemporaryFile> prepared = fileService.prepareTemporaryFiles("textbook", request.attachments());
        try {
            List<UUID> fileIds = fileService.attachPreparedFiles("textbook", null, prepared, request.folderId());
            fileMetadataRepository.flush();
            return fileIds.stream()
                    .map(fileId -> fileMetadataRepository.findById(fileId)
                            .map(FileMetadataResponse::from)
                            .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND)))
                    .toList();
        } catch (RuntimeException exception) {
            fileService.discardPreparedFiles(prepared);
            throw exception;
        }
    }

    @Transactional
    public void recordDownload(UUID fileId, Long userId) {
        if (fileId == null || userId == null) return;

        fileDownloadRepository.upsertDownload(TenantContextHolder.getRequiredTenantId(), fileId, userId);
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
