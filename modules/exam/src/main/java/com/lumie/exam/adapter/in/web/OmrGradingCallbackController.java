package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.dto.response.ExamDetailResponse;
import com.lumie.exam.application.service.ExamQueryService;
import com.lumie.exam.application.service.OmrGradingJobProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/internal/omr")
@RequiredArgsConstructor
public class OmrGradingCallbackController {

    private final OmrGradingJobProcessor processor;
    private final ExamQueryService examQueryService;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody OmrGradingCallbackRequest request) {
        processor.processGradingCallback(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/exams/{id}/detail")
    public ResponseEntity<ExamDetailResponse> getExamDetail(@PathVariable Long id) {
        return ResponseEntity.ok(examQueryService.getExamDetail(id));
    }
}
