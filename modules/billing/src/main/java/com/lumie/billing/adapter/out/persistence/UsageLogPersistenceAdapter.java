package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.UsageLogPersistencePort;
import com.lumie.billing.domain.entity.UsageLog;
import com.lumie.billing.domain.vo.MetricType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsageLogPersistenceAdapter implements UsageLogPersistencePort {

    private final UsageLogJpaRepository usageLogJpaRepository;

    @Override
    public UsageLog save(UsageLog usageLog) {
        return usageLogJpaRepository.save(usageLog);
    }

    @Override
    public Optional<UsageLog> findById(Long id) {
        return usageLogJpaRepository.findById(id);
    }

    @Override
    public Optional<UsageLog> findByTenantIdAndMetricTypeAndRecordedDate(
            Long tenantId, MetricType metricType, LocalDate recordedDate) {
        return usageLogJpaRepository.findByTenantIdAndMetricTypeAndRecordedDate(
                tenantId, metricType, recordedDate);
    }

    @Override
    public List<UsageLog> findByTenantIdAndMetricType(Long tenantId, MetricType metricType) {
        return usageLogJpaRepository.findByTenantIdAndMetricType(tenantId, metricType);
    }

    @Override
    public List<UsageLog> findByTenantIdAndRecordedDateBetween(
            Long tenantId, LocalDate startDate, LocalDate endDate) {
        return usageLogJpaRepository.findByTenantIdAndRecordedDateBetween(tenantId, startDate, endDate);
    }

    @Override
    public Optional<Long> sumValueByTenantIdAndMetricTypeAndRecordedDateBetween(
            Long tenantId, MetricType metricType, LocalDate startDate, LocalDate endDate) {
        return usageLogJpaRepository.sumValueByTenantIdAndMetricTypeAndRecordedDateBetween(
                tenantId, metricType, startDate, endDate);
    }

    @Override
    public Optional<UsageLog> findLatestByTenantIdAndMetricType(Long tenantId, MetricType metricType) {
        return usageLogJpaRepository.findLatestByTenantIdAndMetricType(tenantId, metricType);
    }
}
