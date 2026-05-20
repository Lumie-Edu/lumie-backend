package com.lumie.staff.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StaffRequest(
    @NotBlank(message = "Role is required")
    String role,

    @NotBlank(message = "Name is required")
    String name,

    String phone,
    String email,

    String staffMemo
) {
}
