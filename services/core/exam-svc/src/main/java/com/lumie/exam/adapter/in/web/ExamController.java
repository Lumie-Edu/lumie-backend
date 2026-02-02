package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.CreateExamRequest;
import com.lumie.exam.application.dto.request.UpdateExamRequest;
import com.lumie.exam.application.dto.response.ExamDetailResponse;
import com.lumie.exam.application.dto.response.ExamResponse;
import com.lumie.exam.application.service.ExamCommandService;
import com.lumie.exam.application.service.ExamQueryService;
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
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamCommandService examCommandService;
    private final ExamQueryService examQueryService;

    @GetMapping
    public ResponseEntity<Page<ExamResponse>> listExams(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(examQueryService.listExams(pageable));
    }

    @PostMapping
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody CreateExamRequest request) {
        ExamResponse response = examCommandService.createExam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamResponse> getExam(@PathVariable Long id) {
        return ResponseEntity.ok(examQueryService.getExam(id));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ExamDetailResponse> getExamDetail(@PathVariable Long id) {
        return ResponseEntity.ok(examQueryService.getExamDetail(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamRequest request) {
        return ResponseEntity.ok(examCommandService.updateExam(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examCommandService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
}
