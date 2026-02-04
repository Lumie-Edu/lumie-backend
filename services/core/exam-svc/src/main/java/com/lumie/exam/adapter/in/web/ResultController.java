package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.response.BatchOmrGradingResponse;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.service.ResultCommandService;
import com.lumie.exam.application.service.ResultQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResultController {

    private final ResultCommandService resultCommandService;
    private final ResultQueryService resultQueryService;

    @PostMapping(value = "/exams/{examId}/results/omr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExamResultResponse> processOmrGrading(
            @PathVariable Long examId,
            @RequestParam Long studentId,
            @RequestParam("image") MultipartFile image) throws IOException {
        byte[] imageData = image.getBytes();
        ExamResultResponse response = resultCommandService.processOmrGrading(examId, studentId, imageData);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/exams/{examId}/results/omr/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchOmrGradingResponse> processBatchOmrGrading(
            @PathVariable Long examId,
            @RequestParam("images") List<MultipartFile> images) {
        BatchOmrGradingResponse response = resultCommandService.processBatchOmrGrading(examId, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/exams/{examId}/results")
    public ResponseEntity<List<ExamResultResponse>> getExamResults(@PathVariable Long examId) {
        return ResponseEntity.ok(resultQueryService.getExamResults(examId));
    }

    @GetMapping("/students/{studentId}/results")
    public ResponseEntity<List<ExamResultResponse>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(resultQueryService.getStudentResults(studentId));
    }

    @GetMapping("/results/{resultId}/questions")
    public ResponseEntity<List<QuestionResultResponse>> getQuestionResults(@PathVariable Long resultId) {
        return ResponseEntity.ok(resultQueryService.getQuestionResults(resultId));
    }
}
