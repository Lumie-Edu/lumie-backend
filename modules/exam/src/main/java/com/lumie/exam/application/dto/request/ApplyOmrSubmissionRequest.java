package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record ApplyOmrSubmissionRequest(
        @NotNull Long studentId,
        boolean replaceCurrentResult
) {
}
