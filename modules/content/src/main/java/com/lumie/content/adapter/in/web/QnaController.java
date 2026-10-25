package com.lumie.content.adapter.in.web;

import com.lumie.content.application.dto.request.CreateCommentRequest;
import com.lumie.content.application.dto.request.CreateQnaRequest;
import com.lumie.content.application.dto.request.UpdateQnaRequest;
import com.lumie.content.application.dto.response.QnaBoardResponse;
import com.lumie.content.application.dto.response.QnaCommentResponse;
import com.lumie.content.application.dto.response.QnaDetailResponse;
import com.lumie.content.application.dto.response.QnaUnansweredOverdueResponse;
import com.lumie.content.application.service.QnaCommandService;
import com.lumie.content.application.service.QnaQueryService;
import com.lumie.file.api.FileService;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.common.idempotency.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaCommandService commandService;
    private final QnaQueryService queryService;
    private final FileService fileService;
    private final IdempotencyService idempotencyService;

    @GetMapping
    @Operation(description = "Requires a staff role (OWNER, MANAGER, or INSTRUCTOR).")
    @AllowedSortFields({"createdAt"})
    public ResponseEntity<PageResponse<QnaBoardResponse>> listQnas(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isAnswered,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.listQnas(keyword, isAnswered, pageable)));
    }

    @GetMapping("/me")
    @AllowedSortFields({"createdAt"})
    @Operation(summary = "List Q&As for the current student", description = "Requires the STUDENT role.")
    public ResponseEntity<PageResponse<QnaBoardResponse>> listMyQnas(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isAnswered,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.listMyQnas(keyword, isAnswered, pageable)));
    }

    @GetMapping("/me/{id}")
    @Operation(summary = "Get a Q&A owned by the current student", description = "Requires the student owner.")
    public ResponseEntity<QnaDetailResponse> getMyQna(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getMyQna(id));
    }

    @PostMapping("/me")
    @Operation(summary = "Create a Q&A for the current student", description = "Requires the STUDENT role.")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<QnaBoardResponse> createMyQna(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateQnaRequest request) {
        return idempotencyService.executeOnceAtomically(idempotencyKey, "POST /v1/qna/me", request,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(commandService.createQna(request)));
    }

    @PatchMapping("/me/{id}")
    @Operation(summary = "Update a Q&A owned by the current student", description = "Requires the student owner.")
    public ResponseEntity<QnaBoardResponse> updateMyQna(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQnaRequest request) {
        return ResponseEntity.ok(commandService.updateMyQna(id, request));
    }

    @DeleteMapping("/me/{id}")
    @Operation(summary = "Delete a Q&A owned by the current student", description = "Requires the student owner.")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteMyQna(@PathVariable Long id) {
        commandService.deleteMyQna(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics/unanswered-overdue")
    @Operation(description = "Requires a staff role (OWNER, MANAGER, or INSTRUCTOR).")
    public ResponseEntity<QnaUnansweredOverdueResponse> getUnansweredOverdueStatistics(
            @RequestParam(defaultValue = "24") long thresholdHours) {
        return ResponseEntity.ok(queryService.getUnansweredOverdueStatistics(thresholdHours));
    }

    @GetMapping("/unanswered")
    @Operation(description = "Requires a staff role (OWNER, MANAGER, or INSTRUCTOR).")
    @AllowedSortFields({"createdAt"})
    public ResponseEntity<PageResponse<QnaBoardResponse>> listUnansweredQnas(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.listUnansweredQnas(pageable)));
    }

    @GetMapping("/user/{qnaUserId}")
    @Operation(description = "Requires a staff role (OWNER, MANAGER, or INSTRUCTOR).")
    @AllowedSortFields({"createdAt"})
    public ResponseEntity<PageResponse<QnaBoardResponse>> listQnasByUser(
            @PathVariable Long qnaUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.listQnasByUser(qnaUserId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(description = "Requires the student owner or a staff role.")
    public ResponseEntity<QnaDetailResponse> getQna(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getQna(id));
    }

    @PostMapping
    @Operation(description = "Requires the STUDENT role. The author is derived from the session.")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<QnaBoardResponse> createQna(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateQnaRequest request) {
        return idempotencyService.executeOnceAtomically(idempotencyKey, "POST /v1/qna", request,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(commandService.createQna(request)));
    }

    @PatchMapping("/{id}")
    @Operation(description = "Requires the student owner or a staff role.")
    public ResponseEntity<QnaBoardResponse> updateQna(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQnaRequest request) {
        return ResponseEntity.ok(commandService.updateQna(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(description = "Requires the student owner or a staff role.")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteQna(@PathVariable Long id) {
        commandService.deleteQna(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    @Operation(description = "Requires a staff role (OWNER, MANAGER, or INSTRUCTOR).")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<QnaCommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {
        QnaCommentResponse response = commandService.addComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{qnaId}/comments/{commentId}")
    @Operation(description = "Requires a staff role (OWNER, MANAGER, or INSTRUCTOR).")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long qnaId,
            @PathVariable Long commentId) {
        commandService.deleteComment(qnaId, commentId);
        return ResponseEntity.noContent().build();
    }
}
