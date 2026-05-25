package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.vo.PlanLimits;

public record PlanLimitsResponse(
        int maxStudents,
        int maxAcademies,
        int maxAdmins,
        int omrMonthlyQuota
) {
    public static PlanLimitsResponse from(PlanLimits vo) {
        if (vo == null) {
            return null;
        }
        return new PlanLimitsResponse(
                vo.maxStudents(),
                vo.maxAcademies(),
                vo.maxAdmins(),
                vo.omrMonthlyQuota()
        );
    }
}
