package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.vo.MetricType;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "usage_logs", indexes = {
        @Index(name = "idx_usage_logs_tenant_date", columnList = "tenant_id, recorded_date"),
        @Index(name = "idx_usage_logs_tenant_metric", columnList = "tenant_id, metric_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tenant_slug", nullable = false, length = 30)
    private String tenantSlug;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 30)
    private MetricType metricType;

    @Column(name = "value", nullable = false)
    private long value;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Builder
    private UsageLog(Long tenantId, String tenantSlug, MetricType metricType,
                     long value, LocalDate recordedDate) {
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
        this.metricType = metricType;
        this.value = value;
        this.recordedDate = recordedDate != null ? recordedDate : LocalDate.now();
    }

    public static UsageLog record(Long tenantId, String tenantSlug, MetricType metricType, long value) {
        return UsageLog.builder()
                .tenantId(tenantId)
                .tenantSlug(tenantSlug)
                .metricType(metricType)
                .value(value)
                .recordedDate(LocalDate.now())
                .build();
    }

    public void updateValue(long newValue) {
        this.value = newValue;
    }

    public void incrementValue(long delta) {
        this.value += delta;
    }
}
