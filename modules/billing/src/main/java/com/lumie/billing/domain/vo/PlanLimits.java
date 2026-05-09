package com.lumie.billing.domain.vo;

import lombok.Builder;

@Builder
public record PlanLimits(
        int maxStudents,
        int maxAcademies,
        int maxAdmins,
        int omrMonthlyQuota
) {
    public static PlanLimits free() {
        return PlanLimits.builder()
                .maxStudents(50)
                .maxAcademies(1)
                .maxAdmins(2)
                .omrMonthlyQuota(100)
                .build();
    }

    public static PlanLimits basic() {
        return PlanLimits.builder()
                .maxStudents(200)
                .maxAcademies(3)
                .maxAdmins(5)
                .omrMonthlyQuota(500)
                .build();
    }

    public static PlanLimits pro() {
        return PlanLimits.builder()
                .maxStudents(1000)
                .maxAcademies(10)
                .maxAdmins(20)
                .omrMonthlyQuota(5000)
                .build();
    }

    public static PlanLimits enterprise() {
        return PlanLimits.builder()
                .maxStudents(Integer.MAX_VALUE)
                .maxAcademies(Integer.MAX_VALUE)
                .maxAdmins(Integer.MAX_VALUE)
                .omrMonthlyQuota(Integer.MAX_VALUE)
                .build();
    }

    public boolean isWithinLimit(MetricType metricType, long currentUsage) {
        return switch (metricType) {
            case STUDENTS -> currentUsage < maxStudents;
            case ACADEMIES -> currentUsage < maxAcademies;
            case ADMINS -> currentUsage < maxAdmins;
            case OMR_MONTHLY -> currentUsage < omrMonthlyQuota;
        };
    }

    public int getLimit(MetricType metricType) {
        return switch (metricType) {
            case STUDENTS -> maxStudents;
            case ACADEMIES -> maxAcademies;
            case ADMINS -> maxAdmins;
            case OMR_MONTHLY -> omrMonthlyQuota;
        };
    }
}
