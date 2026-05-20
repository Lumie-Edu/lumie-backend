package com.lumie.admin.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRequest(
    @NotBlank(message = "Role is required")
    String role,

    @NotBlank(message = "Name is required")
    String name,

    String phone,
    String email,

    String adminMemo
) {
}
