package com.lumie.billing.application.service;

import com.lumie.billing.application.port.in.RecordUsageUseCase;
import com.lumie.billing.application.port.out.UsageLogPersistencePort;
import com.lumie.billing.domain.entity.UsageLog;
import com.lumie.billing.domain.vo.MetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsageTrackingService implements RecordUsageUseCase {

    private final UsageLogPersistencePort usageLogPersistencePort;

    @Override
    public void recordUsage(Long tenantId, String tenantSlug, MetricType metricType, long value) {
        log.debug("Recording usage for tenant: {}, metricType: {}, value: {}",
                tenantSlug, metricType, value);

        LocalDate today = LocalDate.now();
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

    @Override
    public void incrementUsage(Long tenantId, String tenantSlug, MetricType metricType, long delta) {
        log.debug("Incrementing usage for tenant: {}, metricType: {}, delta: {}",
                tenantSlug, metricType, delta);

        LocalDate today = LocalDate.now();
        UsageLog usageLog = usageLogPersistencePort
                .findByTenantIdAndMetricTypeAndRecordedDate(tenantId, metricType, today)
                .map(existing -> {
                    existing.incrementValue(delta);
                    return existing;
                })
                .orElseGet(() -> UsageLog.record(tenantId, tenantSlug, metricType, delta));

        usageLogPersistencePort.save(usageLog);
        log.debug("Usage incremented: tenantSlug={}, metricType={}, newValue={}",
                tenantSlug, metricType, usageLog.getValue());
    }
}
