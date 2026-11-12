package com.lumie.exam.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.common.auth.Role;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.dto.request.ExamAnswerRegradeConfirmRequest;
import com.lumie.exam.application.dto.response.ExamAnswerKeyRevisionResponse;
import com.lumie.exam.application.dto.response.ExamAnswerRegradeRevisionResponse;
import com.lumie.exam.application.dto.request.ExamAnswerRegradePreviewRequest;
import com.lumie.exam.application.dto.response.ExamAnswerRegradePreviewResponse;
import com.lumie.exam.application.service.ExamRegradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/exams")
@RequiredArgsConstructor
@Tag(name = "exam-controller")
public class ExamAnswerRegradeController {

    private final ExamRegradeService examRegradeService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/{examId}/answer-key-revisions/preview")
    public ResponseEntity<ExamAnswerRegradePreviewResponse> previewAnswerKeyRevision(
            @PathVariable Long examId,
            @Valid @RequestBody ExamAnswerRegradePreviewRequest request) {
        AuthorizationGuard.requireStaff();
        return ResponseEntity.ok(examRegradeService.preview(examId, request));
    }

    @PostMapping("/{examId}/answer-key-revisions")
    public ResponseEntity<ExamAnswerRegradeRevisionResponse> confirmAnswerKeyRevision(
            @PathVariable Long examId,
            @Valid @RequestBody ExamAnswerRegradeConfirmRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        AuthorizationGuard.requireStaff();
        return idempotencyService.executeOnceAtomically(
                idempotencyKey,
                "POST /v1/exams/{examId}/answer-key-revisions",
                java.util.Map.of("examId", examId, "request", request),
                () -> ResponseEntity.ok(examRegradeService.confirm(
                        examId, request, UserContextHolder.getRequiredUserId())));
    }

    @GetMapping("/{examId}/answer-key-revisions")
    @AllowedSortFields({"createdAt"})
    public ResponseEntity<PageResponse<ExamAnswerKeyRevisionResponse>> listAnswerKeyRevisions(
            @PathVariable Long examId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(PageResponse.from(examRegradeService.history(examId, pageable)));
    }
}
