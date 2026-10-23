package com.lumie.file.application.service;

import com.lumie.file.application.port.out.FileStorageOperationMetricsPersistencePort;
import com.lumie.file.domain.entity.FileStorageOperation;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exports tenant-aggregated file operation metrics with bounded labels only. */
@Component
@RequiredArgsConstructor
public class StorageOperationMetrics {

    private static final String OPERATIONS = "lumie_file_storage_operations";
    private static final String OLDEST_AGE = "lumie_file_storage_oldest_operation_age_seconds";
    private static final String ATTEMPTS = "lumie_file_storage_operation_attempts";
    private static final String TERMINAL_FAILURES = "lumie_file_storage_terminal_failures";
    private static final String EXPIRING = "lumie_file_storage_promotions_expiring_soon";
    private static final Set<String> RESULTS = Set.of("success", "retry", "terminal");
    private static final Set<String> TERMINAL_REASONS = Set.of(
            "source_missing", "source_expired", "metadata_mismatch", "retry_deadline", "attempt_limit", "storage_error");

    private final MeterRegistry meterRegistry;
    private final FileStorageOperationMetricsPersistencePort persistencePort;
    private final ConcurrentHashMap<SeriesKey, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SeriesKey, AtomicLong> oldestAges = new ConcurrentHashMap<>();
    private final AtomicLong promotionsExpiringSoon = new AtomicLong();

    public void beginSnapshotRefresh() {
        operationCounts.values().forEach(value -> value.set(0));
        oldestAges.values().forEach(value -> value.set(0));
        promotionsExpiringSoon.set(0);
    }

    /** Must be called with the tenant context whose rows are being aggregated. */
    public void refreshCurrentTenantSnapshot() {
        var snapshot = persistencePort.snapshot(Instant.now());
        snapshot.operationCounts().forEach(series -> gauge(OPERATIONS, operationCounts, series).addAndGet(series.value()));
        snapshot.oldestOperationAges().forEach(series -> gauge(OLDEST_AGE, oldestAges, series)
                .accumulateAndGet(series.value(), Math::max));
        promotionsExpiringSoon.addAndGet(snapshot.promotionsExpiringSoon());
    }

    public void recordAttempt(FileStorageOperation operation, String result) {
        if (!RESULTS.contains(result)) throw new IllegalArgumentException("Unsupported metric result class");
        meterRegistry.counter(ATTEMPTS,
                "operation", operationTag(operation),
                "result", result).increment();
    }

    public void recordTerminalFailure(FileStorageOperation operation, String reason) {
        if (!TERMINAL_REASONS.contains(reason)) throw new IllegalArgumentException("Unsupported terminal reason label");
        meterRegistry.counter(TERMINAL_FAILURES,
                "operation", operationTag(operation),
                "reason", reason).increment();
    }

    @PostConstruct
    public void initializeMeters() {
        Gauge.builder(EXPIRING, promotionsExpiringSoon, AtomicLong::get)
                .tag("window", "2h")
                .register(meterRegistry);
    }

    private AtomicLong gauge(String name, ConcurrentHashMap<SeriesKey, AtomicLong> values,
                             FileStorageOperationMetricsPersistencePort.Series series) {
        SeriesKey key = new SeriesKey(series.operation(), series.status());
        return values.computeIfAbsent(key, ignored -> {
            AtomicLong value = new AtomicLong();
            Gauge.builder(name, value, AtomicLong::get)
                    .tag("operation", series.operation())
                    .tag("status", series.status())
                    .register(meterRegistry);
            return value;
        });
    }

    private static String operationTag(FileStorageOperation operation) {
        return operation.getOperationType().name().toLowerCase(Locale.ROOT);
    }

    private record SeriesKey(String operation, String status) { }
}
