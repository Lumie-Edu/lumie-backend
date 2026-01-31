package com.lumie.file.adapter.in.web;

import com.lumie.file.application.dto.request.PresignedDownloadRequest;
import com.lumie.file.application.dto.request.PresignedUploadRequest;
import com.lumie.file.application.dto.request.RegisterUploadRequest;
import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.application.dto.response.PresignedDownloadResponse;
import com.lumie.file.application.dto.response.PresignedUploadResponse;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.file.application.service.FileQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileCommandService fileCommandService;
    private final FileQueryService fileQueryService;

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
}
