package com.lumie.file.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.dto.response.FileDownloadResult;
import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.dto.response.TemporaryObjectUploadResponse;
import com.lumie.file.application.dto.response.UploadSessionResponse;
import com.lumie.file.application.dto.response.UploadSessionStatusResponse;
import com.lumie.file.application.dto.request.CreateUploadSessionRequest;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.file.application.service.FileQueryService;
import com.lumie.file.application.service.TemporaryObjectUploadService;
import com.lumie.file.application.service.UploadSessionService;
import com.lumie.file.application.dto.request.CreateTemporaryUploadRequest;
import com.lumie.file.application.dto.request.RegisterTextbookFilesRequest;
import com.lumie.file.domain.vo.EntityType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class FileController {

    private final FileCommandService fileCommandService;
    private final FileQueryService fileQueryService;
    private final TemporaryObjectUploadService temporaryObjectUploadService;
    private final UploadSessionService uploadSessionService;

    /** Compatibility constructor for isolated legacy controller fixtures. */
    public FileController(FileCommandService fileCommandService, FileQueryService fileQueryService,
                          TemporaryObjectUploadService temporaryObjectUploadService) {
        this(fileCommandService, fileQueryService, temporaryObjectUploadService, null);
    }

    @PostMapping("/upload-sessions")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<UploadSessionResponse> createUploadSession(
            @Valid @RequestBody CreateUploadSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadSessionService.create(request));
    }

    @PostMapping("/upload-sessions/{sessionId}/complete")
    public ResponseEntity<UploadSessionStatusResponse> completeUploadSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(uploadSessionService.complete(sessionId));
    }

    @GetMapping("/upload-sessions/{sessionId}")
    public ResponseEntity<UploadSessionStatusResponse> getUploadSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(uploadSessionService.status(sessionId));
    }

    @PostMapping("/upload-sessions/{sessionId}/renew-url")
    public ResponseEntity<UploadSessionResponse> renewUploadSessionUrl(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(uploadSessionService.renew(sessionId));
    }

    @PostMapping("/upload-sessions/{sessionId}/cancel")
    public ResponseEntity<Void> cancelUploadSession(@PathVariable UUID sessionId) {
        uploadSessionService.cancel(sessionId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable UUID fileId) {
        FileDownloadResult result = fileQueryService.downloadFile(fileId);

        Long userId = com.lumie.common.tenant.UserContextHolder.getUserId();
        if (userId != null) {
            fileCommandService.recordDownload(fileId, userId);
        }

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(result.inputStream()));
    }

    @GetMapping("/{fileId}/downloaders")
    public ResponseEntity<List<com.lumie.file.application.dto.response.FileDownloaderResponse>> getFileDownloaders(@PathVariable UUID fileId) {
        AuthorizationGuard.requireStaff();
        List<com.lumie.file.application.dto.response.FileDownloaderResponse> response = fileQueryService.getFileDownloaders(fileId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/uploads")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<TemporaryObjectUploadResponse> createTemporaryUpload(
            @Valid @RequestBody CreateTemporaryUploadRequest request) {
        Role role = UserContextHolder.getUserRole();
        if (role != Role.STUDENT || request.entityType() != EntityType.QNA) {
            AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(temporaryObjectUploadService.create(request));
    }

    @PostMapping("/textbooks")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<List<FileMetadataResponse>> registerTextbookFiles(
            @Valid @RequestBody RegisterTextbookFilesRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileCommandService.registerTextbookFiles(request));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(@PathVariable UUID fileId) {
        FileMetadataResponse response = fileQueryService.getFileMetadata(fileId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{fileId}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        fileCommandService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> getFilesByEntityType(
            @RequestParam EntityType entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        if (entityType == EntityType.TEXTBOOK) {
            AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        }
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
