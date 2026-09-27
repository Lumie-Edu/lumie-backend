package com.lumie.file.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.file.application.dto.request.CreateTemporaryUploadRequest;
import com.lumie.file.application.dto.response.TemporaryObjectUploadResponse;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FilePath;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TemporaryObjectUploadService {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> EXTENSIONS = Set.of("pdf","hwp","hwpx","doc","docx","xls","xlsx","ppt","pptx","txt","csv","jpg","jpeg","png","webp","gif","zip");
    private final StoragePort storage;
    private final TemporaryUploadTokenService tokenService;
    public TemporaryObjectUploadService(StoragePort storage, TemporaryUploadTokenService tokenService) { this.storage = storage; this.tokenService = tokenService; }
    public TemporaryObjectUploadResponse create(CreateTemporaryUploadRequest request) {
        if (request.fileSize() > MAX_BYTES || !isAllowedFilename(request.filename()) || request.contentType().isBlank()
                || !Set.of(EntityType.ANNOUNCEMENT, EntityType.QNA, EntityType.TEXTBOOK).contains(request.entityType()))
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
        String key = FilePath.of(request.entityType(), UUID.randomUUID(), request.filename()).toTemporaryObjectKey();
        return new TemporaryObjectUploadResponse(key, storage.generatePresignedUploadUrl(key, request.contentType(), 900),
                tokenService.issue(request.entityType(), key, request.filename(), request.contentType(), request.fileSize()));
    }
    public static boolean isAllowedFilename(String value) {
        int dot = value.lastIndexOf('.');
        return dot > 0 && !value.contains("/") && !value.contains("\\") && !value.contains("..")
                && EXTENSIONS.contains(value.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
