package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.CreateExamQuestionTypeRequest;
import com.lumie.exam.application.dto.response.ExamQuestionTypeResponse;
import com.lumie.exam.application.service.ExamQuestionTypeService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/exam-question-types")
@RequiredArgsConstructor
public class ExamQuestionTypeController {

    private final ExamQuestionTypeService examQuestionTypeService;

    @GetMapping
    public ResponseEntity<List<ExamQuestionTypeResponse>> listQuestionTypes() {
        return ResponseEntity.ok(examQuestionTypeService.listQuestionTypes());
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(
            responseCode = "403",
            description = "Staff role required",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Question type already exists",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public ResponseEntity<ExamQuestionTypeResponse> createQuestionType(
            @Valid @RequestBody CreateExamQuestionTypeRequest request) {
        ExamQuestionTypeResponse response = examQuestionTypeService.createQuestionType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(
            responseCode = "403",
            description = "Staff role required",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Question type not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public ResponseEntity<Void> deleteQuestionType(@PathVariable Long id) {
        examQuestionTypeService.deleteQuestionType(id);
        return ResponseEntity.noContent().build();
    }
}
