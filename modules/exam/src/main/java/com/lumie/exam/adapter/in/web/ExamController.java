package com.lumie.exam.adapter.in.web;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.exam.application.dto.request.CreateExamRequest;
import com.lumie.exam.application.dto.request.UpdateExamRequest;
import com.lumie.exam.application.dto.response.ExamFullResponse;
import com.lumie.exam.application.dto.response.ExamResponse;
import com.lumie.exam.application.service.ExamCommandService;
import com.lumie.exam.application.service.ExamQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamCommandService examCommandService;
    private final ExamQueryService examQueryService;

    @GetMapping
    @AllowedSortFields({"createdAt", "name", "category"})
    public ResponseEntity<PageResponse<ExamResponse>> listExams(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(examQueryService.listExams(pageable)));
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody CreateExamRequest request) {
        ExamResponse response = examCommandService.createExam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamResponse> getExam(@PathVariable Long id) {
        return ResponseEntity.ok(examQueryService.getExam(id));
    }

    @GetMapping("/{id}/full")
    public ResponseEntity<ExamFullResponse> getExamFull(@PathVariable Long id) {
        return ResponseEntity.ok(examQueryService.getExamFull(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamRequest request) {
        return ResponseEntity.ok(examCommandService.updateExam(id, request));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examCommandService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
}
