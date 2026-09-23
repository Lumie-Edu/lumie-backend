package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmOmrSubmissionReconciliationRequest(
        @NotBlank String phoneNumber,
        @NotNull Long expectedStudentId,
        @Schema(nullable = true) Long expectedCurrentResultId,
        @NotNull Boolean replaceCurrentResult
) {
}
