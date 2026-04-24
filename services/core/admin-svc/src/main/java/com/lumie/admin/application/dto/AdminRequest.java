package com.lumie.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AdminRequest(
    @NotBlank(message = "Role is required")
    String role,

    @NotBlank(message = "Name is required")
    String name,

    String phone,
    String email,

    List<Long> academyIds,
    String adminMemo
) {
}
