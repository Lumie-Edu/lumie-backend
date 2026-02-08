package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.CreateExamTemplateRequest;
import com.lumie.exam.application.dto.request.UpdateExamTemplateRequest;
import com.lumie.exam.application.dto.response.ExamTemplateResponse;
import com.lumie.exam.application.service.ExamTemplateCommandService;
import com.lumie.exam.application.service.ExamTemplateQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam-templates")
@RequiredArgsConstructor
public class ExamTemplateController {

    private final ExamTemplateCommandService examTemplateCommandService;
    private final ExamTemplateQueryService examTemplateQueryService;

    @GetMapping
    public ResponseEntity<Page<ExamTemplateResponse>> listTemplates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(examTemplateQueryService.listTemplates(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamTemplateResponse> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(examTemplateQueryService.getTemplate(id));
    }

    @PostMapping
    public ResponseEntity<ExamTemplateResponse> createTemplate(
            @Valid @RequestBody CreateExamTemplateRequest request) {
        ExamTemplateResponse response = examTemplateCommandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExamTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamTemplateRequest request) {
        return ResponseEntity.ok(examTemplateCommandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        examTemplateCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
