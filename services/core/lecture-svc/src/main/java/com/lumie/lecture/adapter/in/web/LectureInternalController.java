package com.lumie.lecture.adapter.in.web;

import com.lumie.lecture.application.dto.ProcessingCallbackRequest;
import com.lumie.lecture.application.dto.ProcessingStatusUpdate;
import com.lumie.lecture.application.service.LectureCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/lectures")
@RequiredArgsConstructor
public class LectureInternalController {

    private final LectureCommandService commandService;

    @PostMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                              @RequestBody ProcessingStatusUpdate update) {
        commandService.updateProcessingStatus(id, update);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/callback")
    public ResponseEntity<Void> processCallback(@PathVariable Long id,
                                                  @RequestBody ProcessingCallbackRequest request) {
        commandService.handleProcessingCallback(request);
        return ResponseEntity.ok().build();
    }
}
