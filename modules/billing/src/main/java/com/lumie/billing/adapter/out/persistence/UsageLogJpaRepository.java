package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.UsageLog;
import com.lumie.billing.domain.vo.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsageLogJpaRepository extends JpaRepository<UsageLog, Long> {

    Optional<UsageLog> findByTenantIdAndMetricTypeAndRecordedDate(
            Long tenantId, MetricType metricType, LocalDate recordedDate);

    List<UsageLog> findByTenantIdAndMetricType(Long tenantId, MetricType metricType);

    @Query("SELECT u FROM UsageLog u WHERE u.tenantId = :tenantId " +
            "AND u.recordedDate BETWEEN :startDate AND :endDate ORDER BY u.recordedDate")
    List<UsageLog> findByTenantIdAndRecordedDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(u.value) FROM UsageLog u WHERE u.tenantId = :tenantId " +
            "AND u.metricType = :metricType AND u.recordedDate BETWEEN :startDate AND :endDate")
    Optional<Long> sumValueByTenantIdAndMetricTypeAndRecordedDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("metricType") MetricType metricType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT u FROM UsageLog u WHERE u.tenantId = :tenantId AND u.metricType = :metricType " +
            "ORDER BY u.recordedDate DESC LIMIT 1")
    Optional<UsageLog> findLatestByTenantIdAndMetricType(
            @Param("tenantId") Long tenantId,
            @Param("metricType") MetricType metricType);
}
