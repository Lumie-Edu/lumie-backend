package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.OmrBatchConfirmRequest;
import com.lumie.exam.application.dto.request.OmrPresignRequest;
import com.lumie.exam.application.dto.request.UpdateQuestionAnswerRequest;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.OmrJobResponse;
import com.lumie.exam.application.dto.response.OmrJobStatusResponse;
import com.lumie.exam.application.dto.response.OmrPresignResponse;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.service.ResultCommandService;
import com.lumie.exam.application.service.ResultQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1")
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

@PostMapping("/exams/{examId}/results/omr/batch/presign")
    public ResponseEntity<OmrPresignResponse> requestPresignedUrls(
            @PathVariable Long examId,
            @Valid @RequestBody OmrPresignRequest request) {
        OmrPresignResponse response = resultCommandService.generatePresignedUrls(examId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exams/{examId}/results/omr/batch/confirm")
    public ResponseEntity<OmrJobResponse> confirmBatchUpload(
            @PathVariable Long examId,
            @Valid @RequestBody OmrBatchConfirmRequest request) {
        OmrJobResponse response = resultCommandService.confirmBatchOmrGrading(examId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/exams/{examId}/omr-jobs/{jobId}")
    public ResponseEntity<OmrJobStatusResponse> getOmrJobStatus(
            @PathVariable Long examId,
            @PathVariable Long jobId) {
        return ResponseEntity.ok(resultQueryService.getOmrJobStatus(examId, jobId));
    }

    @GetMapping("/exams/{examId}/omr-jobs")
    public ResponseEntity<List<OmrJobStatusResponse>> getOmrJobs(@PathVariable Long examId) {
        return ResponseEntity.ok(resultQueryService.getOmrJobs(examId));
    }

    @GetMapping("/exams/{examId}/results")
    public ResponseEntity<List<ExamResultResponse>> getExamResults(@PathVariable Long examId) {
        return ResponseEntity.ok(resultQueryService.getExamResults(examId));
    }

    @GetMapping("/students/{studentId}/results")
    public ResponseEntity<List<ExamResultResponse>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(resultQueryService.getStudentResults(studentId));
    }

    @PatchMapping("/exams/{examId}/results/{resultId}/questions/{questionResultId}")
    public ResponseEntity<QuestionResultResponse> updateQuestionAnswer(
            @PathVariable Long examId,
            @PathVariable Long resultId,
            @PathVariable Long questionResultId,
            @Valid @RequestBody UpdateQuestionAnswerRequest request) {
        QuestionResultResponse response = resultCommandService.updateQuestionAnswer(
                examId, resultId, questionResultId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/exams/{examId}/results/{resultId}")
    public ResponseEntity<Void> deleteResult(
            @PathVariable Long examId,
            @PathVariable Long resultId) {
        resultCommandService.deleteResult(examId, resultId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/results/{resultId}/questions")
    public ResponseEntity<List<QuestionResultResponse>> getQuestionResults(@PathVariable Long resultId) {
        return ResponseEntity.ok(resultQueryService.getQuestionResults(resultId));
    }

    @GetMapping("/exams/{examId}/results/{resultId}/omr-image")
    public ResponseEntity<byte[]> getOmrImage(
            @PathVariable Long examId,
            @PathVariable Long resultId) {
        byte[] imageData = resultQueryService.getOmrImageData(resultId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(imageData.length)
                .body(imageData);
    }
}
