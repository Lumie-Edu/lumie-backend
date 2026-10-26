package com.lumie.content.adapter.in.web;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.lumie.content.application.dto.request.CreateAnnouncementRequest;
import com.lumie.content.application.dto.request.UpdateAnnouncementRequest;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.application.service.AnnouncementCommandService;
import com.lumie.content.application.service.AnnouncementQueryService;
import com.lumie.file.api.FileService;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.common.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementCommandService commandService;
    private final AnnouncementQueryService queryService;
    private final FileService fileService;
    private final IdempotencyService idempotencyService;

    @GetMapping
    @AllowedSortFields({"createdAt", "announcementTitle"})
    public ResponseEntity<PageResponse<AnnouncementResponse>> listAnnouncements(
            @RequestParam(required = false) Boolean isAsset,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.listAnnouncements(isAsset, pageable)));
    }

    @GetMapping("/important")
    public ResponseEntity<List<AnnouncementResponse>> listImportantAnnouncements() {
        return ResponseEntity.ok(queryService.listImportantAnnouncements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> getAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getAnnouncement(id));
    }

    @GetMapping("/{id}/readers")
    public ResponseEntity<List<String>> getAnnouncementReaders(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getAnnouncementReaders(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return idempotencyService.executeOnceAtomically(idempotencyKey, "POST /v1/announcements", request,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(commandService.createAnnouncement(request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAnnouncementRequest request) {
        return ResponseEntity.ok(commandService.updateAnnouncement(id, request));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        commandService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }
}
