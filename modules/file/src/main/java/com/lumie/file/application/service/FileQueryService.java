package com.lumie.file.application.service;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.file.application.dto.response.FileDownloadResult;
import com.lumie.file.application.dto.response.FileMetadataResponse;
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

    private static final String DELETED_STUDENT_NAME = "삭제된 학생";

    private final FileMetadataRepository fileMetadataRepository;
    private final com.lumie.file.domain.repository.FileDownloadRepository fileDownloadRepository;
    private final com.lumie.student.api.StudentService studentService;
    private final FileMetadataLookupService fileMetadataLookupService;
    private final StoragePort storagePort;

    public FileDownloadResult downloadFile(UUID fileId) {
        FileMetadataLookupService.DownloadMetadata metadata =
                fileMetadataLookupService.getAuthorizedDownloadMetadata(fileId);

        InputStream stream = storagePort.getObject(metadata.objectKey());
        log.debug("Streaming download for file: {}", fileId);

        return new FileDownloadResult(
                stream,
                metadata.originalFilename(),
                metadata.contentType(),
                metadata.fileSize()
        );
    }

    @Transactional(readOnly = true)
    public FileMetadataResponse getFileMetadata(UUID fileId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        fileMetadataLookupService.requireAccess(fileMetadata.getEntityType(), fileMetadata.getEntityId());

        if (!fileMetadata.isReady()) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }

        return FileMetadataResponse.from(fileMetadata);
    }

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getFilesByEntity(EntityType entityType, Long entityId) {
        fileMetadataLookupService.requireAccess(entityType, entityId);
        return fileMetadataRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .filter(this::isReady)
                .map(FileMetadataResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getFilesByEntityAndFolder(EntityType entityType, UUID folderId) {
        fileMetadataLookupService.requireAccess(entityType, null);
        List<FileMetadata> files = fileMetadataRepository.findByEntityTypeAndFolderId(entityType, folderId);

        return files.stream()
                .filter(this::isReady)
                .map(FileMetadataResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getAllFilesByEntityType(EntityType entityType) {
        fileMetadataLookupService.requireAccess(entityType, null);
        List<FileMetadata> files = fileMetadataRepository.findByEntityType(entityType);

        return files.stream()
                .filter(this::isReady)
                .map(FileMetadataResponse::from)
                .toList();
    }

    private boolean isReady(FileMetadata metadata) {
        return metadata.isReady();
    }

    @Transactional(readOnly = true)
    public List<com.lumie.file.application.dto.response.FileDownloaderResponse> getFileDownloaders(UUID fileId) {
        AuthorizationGuard.requireStaff();
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        List<com.lumie.file.domain.entity.FileDownload> downloads = fileDownloadRepository.findByFileIdOrderByUpdatedAtDesc(fileId);
        if (downloads.isEmpty()) return List.of();

        List<Long> userIds = downloads.stream().map(d -> d.getUserId()).toList();
        String tenantSlug = com.lumie.common.tenant.TenantContextHolder.getRequiredTenant();

        var studentDataList = studentService.getStudentsByUserIds(tenantSlug, userIds).students();
        java.util.Map<Long, String> nameMap = studentDataList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.lumie.student.api.StudentService.StudentData::userId,
                        com.lumie.student.api.StudentService.StudentData::studentName,
                        (a, b) -> a
                ));

        return downloads.stream()
                .map(d -> new com.lumie.file.application.dto.response.FileDownloaderResponse(
                        d.getUserId(),
                        nameMap.getOrDefault(d.getUserId(), DELETED_STUDENT_NAME),
                        d.getUpdatedAt()
                ))
                .toList();
    }



}
