package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.service.OmrGradingJobProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/omr")
@RequiredArgsConstructor
public class OmrGradingCallbackController {

    private final OmrGradingJobProcessor processor;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody OmrGradingCallbackRequest request) {
        processor.processGradingCallback(request);
        return ResponseEntity.ok().build();
    }
}
