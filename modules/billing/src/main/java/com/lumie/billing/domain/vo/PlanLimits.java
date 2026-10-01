package com.lumie.billing.domain.vo;

import com.lumie.common.quota.MetricType;
import lombok.Builder;

/**
 * Immutable snapshot of a plan's usage limits.
 * A {@code null} value for any dimension means "unlimited" — no cap enforced.
 * This mirrors V28 schema: max_students / max_academies / max_admins / omr_monthly_quota
 * are all nullable (NULL = unlimited).
 */
@Builder
public record PlanLimits(
        Integer maxStudents,
        Integer maxAcademies,
        Integer maxStaff,
        Integer omrMonthlyQuota
) {
    public static PlanLimits free() {
        return PlanLimits.builder()
                .maxStudents(50)
                .maxAcademies(1)
                .maxStaff(2)
                .omrMonthlyQuota(100)
                .build();
    }

    public static PlanLimits basic() {
        return PlanLimits.builder()
                .maxStudents(200)
                .maxAcademies(3)
                .maxStaff(5)
                .omrMonthlyQuota(500)
                .build();
    }

    public static PlanLimits pro() {
        return PlanLimits.builder()
                .maxStudents(1000)
                .maxAcademies(10)
                .maxStaff(20)
                .omrMonthlyQuota(5000)
                .build();
    }

    public static PlanLimits unlimited() {
        return PlanLimits.builder()
                .maxStudents(null)
                .maxAcademies(null)
                .maxStaff(null)
                .omrMonthlyQuota(null)
                .build();
    }

    /**
     * Returns {@code true} when the current usage is within the plan's limit.
     * A {@code null} limit means unlimited — always returns {@code true}.
     */
    public boolean isWithinLimit(MetricType metricType, long currentUsage) {
        Integer limit = limitFor(metricType);
        return limit == null || currentUsage < limit;
    }

    /**
     * Returns the numeric limit for the given metric, or {@code null} if unlimited.
     */
    public Integer getLimit(MetricType metricType) {
        return limitFor(metricType);
    }

    private Integer limitFor(MetricType metricType) {
        return switch (metricType) {
            case STUDENTS -> maxStudents;
            case ACADEMIES -> maxAcademies;
            case STAFF -> maxStaff;
            case OMR_MONTHLY -> omrMonthlyQuota;
        };
    }
}
