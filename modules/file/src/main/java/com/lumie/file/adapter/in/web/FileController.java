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
import com.lumie.file.domain.vo.EntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileCommandService fileCommandService;
    private final FileQueryService fileQueryService;

    @PostMapping("/upload")
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") EntityType entityType,
            @RequestParam(value = "entityId", required = false) Long entityId,
            @RequestParam(value = "folderId", required = false) UUID folderId) {
        FileMetadataResponse response = fileCommandService.uploadFile(entityType, entityId, folderId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable UUID fileId) {
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
}
