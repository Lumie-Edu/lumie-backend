package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.response.ExamDetailResponse;
import com.lumie.exam.application.service.ExamQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal HTTP query endpoints for the OMR domain — used by
 * grading-svc to fetch exam metadata mid-job. Results flow over
 * RabbitMQ (queue {@code grading.omr-callback}) consumed by
 * {@code OmrGradingCallbackListener}.
 */
@Slf4j
@RestController
@RequestMapping("/internal/omr")
@RequiredArgsConstructor
public class InternalOmrController {

    private final ExamQueryService examQueryService;

    @GetMapping("/exams/{id}/full")
    public ResponseEntity<ExamDetailResponse> getExamFull(@PathVariable Long id) {
        return ResponseEntity.ok(examQueryService.getExamFull(id));
    }
}
