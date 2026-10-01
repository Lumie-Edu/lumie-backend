package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.vo.PlanLimits;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Wire representation of {@link PlanLimits}.
 *
 * <p>Each field is nullable — {@code null} = "unlimited on this dimension",
 * mirroring the domain VO (which mirrors the V28 schema). Encoding unlimited
 * as nullable rather than a sentinel (-1, Integer.MAX_VALUE) keeps the
 * contract honest with the BE plan catalog and lets the FE render "무제한"
 * explicitly instead of guessing what a huge number means.
 */
public record PlanLimitsResponse(
        @Schema(nullable = true, description = "학생 한도 (null = 무제한)") Integer maxStudents,
        @Schema(nullable = true, description = "학원 한도 (null = 무제한)") Integer maxAcademies,
        @Schema(nullable = true, description = "직원 한도 (null = 무제한)") Integer maxStaff,
        @Schema(nullable = true, description = "월 OMR 처리 한도 (null = 무제한)") Integer omrMonthlyQuota
) {
    public static PlanLimitsResponse from(PlanLimits vo) {
        if (vo == null) {
            return null;
        }
        return new PlanLimitsResponse(
                vo.maxStudents(),
                vo.maxAcademies(),
                vo.maxStaff(),
                vo.omrMonthlyQuota()
        );
    }
}
