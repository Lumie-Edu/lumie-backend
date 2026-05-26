package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.UsageLogPersistencePort;
import com.lumie.billing.domain.entity.UsageLog;
import com.lumie.common.quota.MetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsageTrackingService {

    private final UsageLogPersistencePort usageLogPersistencePort;

    public void recordUsage(Long tenantId, String tenantSlug, MetricType metricType, long value) {
        log.debug("Recording usage for tenant: {}, metricType: {}, value: {}",
                tenantSlug, metricType, value);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UsageLog usageLog = usageLogPersistencePort
                .findByTenantIdAndMetricTypeAndRecordedDate(tenantId, metricType, today)
                .map(existing -> {
                    existing.updateValue(value);
                    return existing;
                })
                .orElseGet(() -> UsageLog.record(tenantId, tenantSlug, metricType, value));

        usageLogPersistencePort.save(usageLog);
        log.debug("Usage recorded: tenantSlug={}, metricType={}, value={}",
                tenantSlug, metricType, value);
    }

    public void incrementUsage(Long tenantId, String tenantSlug, MetricType metricType, long delta) {
        log.debug("Incrementing usage for tenant: {}, metricType: {}, delta: {}",
                tenantSlug, metricType, delta);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // Single atomic DB operation: inserts on first call of the day, increments on subsequent calls.
        // No application-level lock or retry needed — ON CONFLICT handles both cases atomically.
        usageLogPersistencePort.atomicIncrement(tenantId, tenantSlug, metricType, delta, today);

        log.debug("Usage incremented: tenantSlug={}, metricType={}, delta={}",
                tenantSlug, metricType, delta);
    }
}
