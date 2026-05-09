package com.lumie.file.adapter.in.web;

import com.lumie.file.application.dto.request.CreateTextbookFolderRequest;
import com.lumie.file.application.dto.request.RenameTextbookFolderRequest;
import com.lumie.file.application.dto.response.TextbookFolderPathResponse;
import com.lumie.file.application.dto.response.TextbookFolderResponse;
import com.lumie.file.application.service.TextbookFolderCommandService;
import com.lumie.file.application.service.TextbookFolderQueryService;
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
    public ResponseEntity<TextbookFolderResponse> createFolder(
            @Valid @RequestBody CreateTextbookFolderRequest request) {
        TextbookFolderResponse response = textbookFolderCommandService.createFolder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TextbookFolderResponse>> listFolders(
            @RequestParam(required = false) UUID parentId) {
        List<TextbookFolderResponse> response = textbookFolderQueryService.listFolders(parentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TextbookFolderResponse> getFolder(@PathVariable UUID id) {
        TextbookFolderResponse response = textbookFolderQueryService.getFolder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<TextbookFolderPathResponse> getFolderPath(@PathVariable UUID id) {
        TextbookFolderPathResponse response = textbookFolderQueryService.getFolderPath(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TextbookFolderResponse> renameFolder(
            @PathVariable UUID id,
            @Valid @RequestBody RenameTextbookFolderRequest request) {
        TextbookFolderResponse response = textbookFolderCommandService.renameFolder(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID id) {
        textbookFolderCommandService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }
}
