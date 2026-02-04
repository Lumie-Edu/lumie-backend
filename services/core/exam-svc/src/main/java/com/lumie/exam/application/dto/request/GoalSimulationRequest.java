package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GoalSimulationRequest(
        @NotNull(message = "목표 등급은 필수입니다")
        @Min(value = 1, message = "등급은 1 이상이어야 합니다")
        @Max(value = 9, message = "등급은 9 이하이어야 합니다")
        Integer targetGrade,

        Long baseExamId
) {}
