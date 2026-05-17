package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.UsageLog;
import com.lumie.billing.domain.vo.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsageLogJpaRepository extends JpaRepository<UsageLog, Long> {

    Optional<UsageLog> findByTenantIdAndMetricTypeAndRecordedDate(
            Long tenantId, MetricType metricType, LocalDate recordedDate);

    /**
     * Atomically inserts or increments today's usage row.
     * ON CONFLICT ... DO UPDATE is a single atomic DB operation — no application-level
     * lock or retry needed. metricType is passed as its string name (enum.name()).
     */
    @Modifying
    @Query(value = """
            INSERT INTO usage_logs
                (tenant_id, tenant_slug, metric_type, value, recorded_date, version, created_at, updated_at)
            VALUES
                (:tenantId, :tenantSlug, :metricType, :delta, :date, 0, NOW(), NOW())
            ON CONFLICT (tenant_id, metric_type, recorded_date) DO UPDATE
                SET value      = usage_logs.value + EXCLUDED.value,
                    version    = usage_logs.version + 1,
                    updated_at = NOW()
            """, nativeQuery = true)
    void atomicIncrement(
            @Param("tenantId") Long tenantId,
            @Param("tenantSlug") String tenantSlug,
            @Param("metricType") String metricType,
            @Param("delta") long delta,
            @Param("date") LocalDate date);

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
