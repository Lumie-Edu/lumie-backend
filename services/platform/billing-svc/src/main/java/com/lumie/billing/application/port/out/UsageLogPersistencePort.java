package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.UsageLog;
import com.lumie.billing.domain.vo.MetricType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsageLogPersistencePort {
    UsageLog save(UsageLog usageLog);
    Optional<UsageLog> findById(Long id);
    Optional<UsageLog> findByTenantIdAndMetricTypeAndRecordedDate(
            Long tenantId, MetricType metricType, LocalDate recordedDate);
    List<UsageLog> findByTenantIdAndMetricType(Long tenantId, MetricType metricType);
    List<UsageLog> findByTenantIdAndRecordedDateBetween(
            Long tenantId, LocalDate startDate, LocalDate endDate);
    Optional<Long> sumValueByTenantIdAndMetricTypeAndRecordedDateBetween(
            Long tenantId, MetricType metricType, LocalDate startDate, LocalDate endDate);
    Optional<UsageLog> findLatestByTenantIdAndMetricType(Long tenantId, MetricType metricType);
}
