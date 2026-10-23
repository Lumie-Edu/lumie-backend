package com.lumie.file.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.auth.Role;
import com.lumie.file.application.dto.request.CreateTextbookFolderRequest;
import com.lumie.file.application.dto.request.RenameTextbookFolderRequest;
import com.lumie.file.application.dto.response.TextbookFolderPathResponse;
import com.lumie.file.application.dto.response.TextbookFolderResponse;
import com.lumie.file.application.service.TextbookFolderCommandService;
import com.lumie.file.application.service.TextbookFolderQueryService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/textbook-folders")
@RequiredArgsConstructor
public class TextbookFolderController {

    private final TextbookFolderCommandService textbookFolderCommandService;
    private final TextbookFolderQueryService textbookFolderQueryService;

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<TextbookFolderResponse> createFolder(
            @Valid @RequestBody CreateTextbookFolderRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        TextbookFolderResponse response = textbookFolderCommandService.createFolder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TextbookFolderResponse>> listFolders(
            @RequestParam(required = false) UUID parentId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        List<TextbookFolderResponse> response = textbookFolderQueryService.listFolders(parentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TextbookFolderResponse> getFolder(@PathVariable UUID id) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        TextbookFolderResponse response = textbookFolderQueryService.getFolder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<TextbookFolderPathResponse> getFolderPath(@PathVariable UUID id) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        TextbookFolderPathResponse response = textbookFolderQueryService.getFolderPath(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TextbookFolderResponse> renameFolder(
            @PathVariable UUID id,
            @Valid @RequestBody RenameTextbookFolderRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        TextbookFolderResponse response = textbookFolderCommandService.renameFolder(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID id) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        textbookFolderCommandService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }
}
