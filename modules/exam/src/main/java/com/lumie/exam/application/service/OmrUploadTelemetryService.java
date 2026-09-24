package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.OmrUploadTelemetryRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmrUploadTelemetryService {

    private final MeterRegistry meterRegistry;

    public void record(Long examId, OmrUploadTelemetryRequest request) {
        String connectionType = metricConnectionType(request.connectionType());

        Timer.builder("omr.upload.client.batch.duration")
                .tag("outcome", batchOutcome(request))
                .tag("connection_type", connectionType)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofMillis(request.batchDurationMs()));
        DistributionSummary.builder("omr.upload.client.batch.bytes")
                .tag("connection_type", connectionType)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(request.totalBytes());

        for (OmrUploadTelemetryRequest.OmrUploadTelemetryFile file : request.files()) {
            String outcome = file.outcome().name().toLowerCase();
            Timer.builder("omr.upload.client.file.duration")
                    .tag("outcome", outcome)
                    .tag("connection_type", connectionType)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(Duration.ofMillis(file.durationMs()));
            DistributionSummary.builder("omr.upload.client.file.bytes")
                    .tag("outcome", outcome)
                    .tag("connection_type", connectionType)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(file.uploadedBytes());
            if (file.outcome() != OmrUploadTelemetryRequest.OmrUploadOutcome.SUCCESS) {
                Counter.builder("omr.upload.client.file.failures")
                        .tag("outcome", outcome)
                        .tag("http_status", metricStatus(file.httpStatus()))
                        .register(meterRegistry)
                        .increment();
            }
            log.atInfo()
                    .addKeyValue("eventType", "omr_upload_client")
                    .addKeyValue("examId", examId)
                    .addKeyValue("batchKey", request.batchKey())
                    .addKeyValue("imageIndex", file.imageIndex())
                    .addKeyValue("totalBytes", file.totalBytes())
                    .addKeyValue("uploadedBytes", file.uploadedBytes())
                    .addKeyValue("durationMs", file.durationMs())
                    .addKeyValue("httpStatus", file.httpStatus())
                    .addKeyValue("outcome", outcome)
                    .addKeyValue("connectionType", connectionType)
                    .log("OMR client upload file completed");
        }
    }

    private static String batchOutcome(OmrUploadTelemetryRequest request) {
        return request.files().stream()
                .allMatch(file -> file.outcome() == OmrUploadTelemetryRequest.OmrUploadOutcome.SUCCESS)
                ? "success"
                : "partial_failure";
    }

    private static String metricConnectionType(String connectionType) {
        if (connectionType == null || connectionType.isBlank()) {
            return "unknown";
        }
        return connectionType.matches("[a-zA-Z0-9_-]{1,24}") ? connectionType : "other";
    }

    private static String metricStatus(int httpStatus) {
        return httpStatus == 0 ? "network" : Integer.toString(httpStatus);
    }
}
