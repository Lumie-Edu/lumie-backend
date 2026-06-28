package com.lumie.notification.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSmsSenderSettingApprovalRequest(
        @NotNull(message = "Registration status is required")
        ApprovalStatus registrationStatus,
        @Size(max = 2000, message = "Rejection reason must be at most 2000 characters")
        String rejectionReason
) {
    public enum ApprovalStatus {
        APPROVED,
        REJECTED
    }
}
