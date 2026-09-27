package com.lumie.file.adapter.in.web;

import com.lumie.file.application.dto.response.FileDownloadResult;
import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.dto.response.TemporaryObjectUploadResponse;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.file.application.service.FileQueryService;
import com.lumie.file.application.service.TemporaryObjectUploadService;
import com.lumie.file.application.dto.request.CreateTemporaryUploadRequest;
import com.lumie.file.application.dto.request.RegisterTextbookFilesRequest;
import com.lumie.file.domain.vo.EntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileCommandService fileCommandService;
    private final FileQueryService fileQueryService;
    private final TemporaryObjectUploadService temporaryObjectUploadService;
    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable UUID fileId) {
        FileDownloadResult result = fileQueryService.downloadFile(fileId);

        Long userId = com.lumie.common.tenant.UserContextHolder.getUserId();
        if (userId != null) {
            fileCommandService.recordDownload(fileId, userId);
        }

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

    @PostMapping("/uploads")
    public ResponseEntity<TemporaryObjectUploadResponse> createTemporaryUpload(
            @Valid @RequestBody CreateTemporaryUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(temporaryObjectUploadService.create(request));
    }

    @PostMapping("/textbooks")
    public ResponseEntity<List<FileMetadataResponse>> registerTextbookFiles(
            @Valid @RequestBody RegisterTextbookFilesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileCommandService.registerTextbookFiles(request));
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
