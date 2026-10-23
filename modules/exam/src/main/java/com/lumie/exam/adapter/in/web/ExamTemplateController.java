package com.lumie.exam.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.lumie.exam.application.dto.request.CreateExamTemplateRequest;
import com.lumie.exam.application.dto.request.UpdateExamTemplateRequest;
import com.lumie.exam.application.dto.response.ExamTemplateResponse;
import com.lumie.exam.application.service.ExamTemplateCommandService;
import com.lumie.exam.application.service.ExamTemplateQueryService;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/exam-templates")
@RequiredArgsConstructor
public class ExamTemplateController {

    private final ExamTemplateCommandService examTemplateCommandService;
    private final ExamTemplateQueryService examTemplateQueryService;

    @GetMapping
    @AllowedSortFields({"createdAt", "name", "category"})
    public ResponseEntity<PageResponse<ExamTemplateResponse>> listTemplates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AuthorizationGuard.requireStaff();
        return ResponseEntity.ok(PageResponse.from(examTemplateQueryService.listTemplates(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamTemplateResponse> getTemplate(@PathVariable Long id) {
        AuthorizationGuard.requireStaff();
        return ResponseEntity.ok(examTemplateQueryService.getTemplate(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<ExamTemplateResponse> createTemplate(
            @Valid @RequestBody CreateExamTemplateRequest request) {
        AuthorizationGuard.requireStaff();
        ExamTemplateResponse response = examTemplateCommandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExamTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamTemplateRequest request) {
        AuthorizationGuard.requireStaff();
        return ResponseEntity.ok(examTemplateCommandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        AuthorizationGuard.requireStaff();
        examTemplateCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
