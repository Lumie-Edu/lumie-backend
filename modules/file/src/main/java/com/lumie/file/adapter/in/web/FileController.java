package com.lumie.file.adapter.in.web;

import com.lumie.file.application.dto.request.PresignedDownloadRequest;
import com.lumie.file.application.dto.request.PresignedUploadRequest;
import com.lumie.file.application.dto.request.RegisterUploadRequest;
import com.lumie.file.application.dto.response.FileDownloadResult;
import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.dto.response.PresignedDownloadResponse;
import com.lumie.file.application.dto.response.PresignedUploadResponse;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.file.application.service.FileQueryService;
import com.lumie.common.exception.BusinessRuleViolationException;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.file.domain.vo.EntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileCommandService fileCommandService;
    private final FileQueryService fileQueryService;
    private final IdempotencyService idempotencyService;

    private static final long PROXY_UPLOAD_MAX_BYTES = 1024 * 1024; // 1 MiB
    private static final String UPLOAD_SCOPE = "POST /v1/files/upload";

    @PostMapping("/upload")
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") EntityType entityType,
            @RequestParam(value = "entityId", required = false) Long entityId,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws IOException {
        if (file.getSize() > PROXY_UPLOAD_MAX_BYTES) {
            throw new BusinessRuleViolationException(
                "Files larger than 1 MiB must use the presigned-upload flow (POST /v1/files/presigned-upload)");
        }
        if (idempotencyKey != null) {
            byte[] bytes = file.getBytes();
            UploadFingerprint fingerprint = new UploadFingerprint(
                    entityType.name(),
                    entityId != null ? entityId.toString() : "",
                    folderId != null ? folderId.toString() : "",
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
                    file.getContentType() != null ? file.getContentType() : "",
                    bytes.length,
                    sha256Hex(bytes)
            );
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    UPLOAD_SCOPE,
                    fingerprint,
                    () -> {
                        FileMetadataResponse response = fileCommandService.uploadFile(entityType, entityId, folderId, file);
                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    }
            );
        }
        FileMetadataResponse response = fileCommandService.uploadFile(entityType, entityId, folderId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable UUID fileId) {
        Long userId = com.lumie.common.tenant.UserContextHolder.getUserId();
        if (userId != null) {
            fileCommandService.recordDownload(fileId, userId);
        }

        FileDownloadResult result = fileQueryService.downloadFile(fileId);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(result.inputStream()));
    }

    @GetMapping("/{fileId}/downloaders")
    public ResponseEntity<List<com.lumie.file.application.dto.response.FileDownloaderResponse>> getFileDownloaders(@PathVariable UUID fileId) {
        List<com.lumie.file.application.dto.response.FileDownloaderResponse> response = fileQueryService.getFileDownloaders(fileId);
        return ResponseEntity.ok(response);
    }

    private static final java.util.Set<EntityType> PUBLIC_ALLOWED_TYPES =
            java.util.Set.of(EntityType.HOMEPAGE_ASSET);

    @GetMapping("/public/{fileId}")
    public ResponseEntity<InputStreamResource> downloadPublicFile(@PathVariable UUID fileId) {
        FileDownloadResult result = fileQueryService.downloadPublicFile(fileId, PUBLIC_ALLOWED_TYPES);

        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(result.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .body(new InputStreamResource(result.inputStream()));
    }

    @PostMapping("/presigned-upload")
    public ResponseEntity<PresignedUploadResponse> generatePresignedUploadUrl(
            @Valid @RequestBody PresignedUploadRequest request) {
        PresignedUploadResponse response = fileCommandService.generatePresignedUploadUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/presigned-download")
    public ResponseEntity<PresignedDownloadResponse> generatePresignedDownloadUrl(
            @Valid @RequestBody PresignedDownloadRequest request) {
        PresignedDownloadResponse response = fileQueryService.generatePresignedDownloadUrl(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<FileMetadataResponse> registerUploadCompleted(
            @Valid @RequestBody RegisterUploadRequest request) {
        FileMetadataResponse response = fileCommandService.registerUploadCompleted(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(@PathVariable UUID fileId) {
        FileMetadataResponse response = fileQueryService.getFileMetadata(fileId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId) {
        fileCommandService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> getFilesByEntityType(
            @RequestParam EntityType entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        List<FileMetadataResponse> response;
        if (entityType == EntityType.TEXTBOOK && all) {
            response = fileQueryService.getAllFilesByEntityType(entityType);
        } else if (entityType == EntityType.TEXTBOOK && (folderId != null || entityId == null)) {
            response = fileQueryService.getFilesByEntityAndFolder(entityType, folderId);
        } else {
            response = fileQueryService.getFilesByEntity(entityType, entityId);
        }
        return ResponseEntity.ok(response);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java spec — unreachable.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Internal fingerprint payload for {@link IdempotencyService#executeOnce}.
     * Stable JSON serialization keys the dedup decision on file identity
     * (entity context + filename + size + content-bytes SHA-256), so two
     * uploads with the same {@code Idempotency-Key} but different content
     * trigger a 409 IdempotencyConflictException at the service boundary.
     */
    private record UploadFingerprint(
            String entityType,
            String entityId,
            String folderId,
            String filename,
            String contentType,
            long fileSize,
            String fileSha256
    ) {}
}
