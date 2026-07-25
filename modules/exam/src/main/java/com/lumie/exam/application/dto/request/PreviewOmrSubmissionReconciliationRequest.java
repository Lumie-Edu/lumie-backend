package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PreviewOmrSubmissionReconciliationRequest(@NotBlank String phoneNumber) {
}
