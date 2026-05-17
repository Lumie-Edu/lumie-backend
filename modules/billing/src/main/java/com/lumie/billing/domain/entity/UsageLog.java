package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.vo.MetricType;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Entity
@Table(name = "usage_logs",
        indexes = {
                @Index(name = "idx_usage_logs_tenant_date", columnList = "tenant_id, recorded_date"),
                @Index(name = "idx_usage_logs_tenant_metric", columnList = "tenant_id, metric_type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_usage_logs_tenant_metric_date",
                        columnNames = {"tenant_id", "metric_type", "recorded_date"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Safety net for non-upsert update paths (e.g. recordUsage). Does not guard
    // incrementUsage — that path uses an atomic ON CONFLICT DO UPDATE instead.
    @Version
    private Long version;

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
        this.recordedDate = recordedDate != null ? recordedDate : LocalDate.now(ZoneOffset.UTC);
    }

    public static UsageLog record(Long tenantId, String tenantSlug, MetricType metricType, long value) {
        return UsageLog.builder()
                .tenantId(tenantId)
                .tenantSlug(tenantSlug)
                .metricType(metricType)
                .value(value)
                .recordedDate(LocalDate.now(ZoneOffset.UTC))
                .build();
    }

    public void updateValue(long newValue) {
        this.value = newValue;
    }

    public void incrementValue(long delta) {
        this.value += delta;
    }
}
