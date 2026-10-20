package com.lumie.file.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.api.ContentService;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileMetadataLookupService {

    private final FileMetadataRepository fileMetadataRepository;
    private final ContentService contentService;

    @Transactional(readOnly = true)
    public DownloadMetadata getAuthorizedDownloadMetadata(UUID fileId) {
        FileMetadata fileMetadata = findCompletedFile(fileId);
        requireAccess(fileMetadata.getEntityType(), fileMetadata.getEntityId());
        return DownloadMetadata.from(fileMetadata);
    }

    private FileMetadata findCompletedFile(UUID fileId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        if (!fileMetadata.isReady()) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND, "File upload not completed");
        }
        return fileMetadata;
    }

    public void requireAccess(EntityType entityType, Long entityId) {
        Role role = UserContextHolder.getUserRole();
        if (role == null) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (role != Role.STUDENT) {
            return;
        }

        if (entityType == EntityType.ANNOUNCEMENT
                && entityId != null
                && contentService.canUserViewAnnouncement(entityId, role.name(), UserContextHolder.getUserId())) {
            return;
        }

        if (entityType == EntityType.QNA
                && entityId != null
                && contentService.canUserViewQna(entityId, role.name(), UserContextHolder.getUserId())) {
            return;
        }

        throw new BusinessException(FileErrorCode.ACCESS_DENIED);
    }

    public record DownloadMetadata(
            UUID id,
            String objectKey,
            String originalFilename,
            String contentType,
            Long fileSize
    ) {
        private static DownloadMetadata from(FileMetadata fileMetadata) {
            return new DownloadMetadata(
                    fileMetadata.getId(),
                    fileMetadata.getObjectKey(),
                    fileMetadata.getOriginalFilename(),
                    fileMetadata.getContentType(),
                    fileMetadata.getFileSize()
            );
        }
    }
}
