package com.lumie.activitylog.adapter.in.web;

import com.lumie.activitylog.application.dto.request.WebVitalsReportRequest;
import com.lumie.activitylog.application.service.WebVitalsRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingests frontend Core Web Vitals beacons. Public (permitAll) — beacons fire from unauthenticated
 * marketing/landing pages too. Returns 204 immediately; the body is recorded to Micrometer.
 */
@RestController
@RequestMapping("/v1/telemetry")
@RequiredArgsConstructor
public class WebVitalsController {

    private final WebVitalsRecordingService webVitalsRecordingService;

    @PostMapping("/web-vitals")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // No @Valid: malformed/partial beacons are normalized in WebVitalsRecordingService, not rejected.
    public void reportWebVitals(@RequestBody WebVitalsReportRequest request) {
        webVitalsRecordingService.record(request);
    }
}
