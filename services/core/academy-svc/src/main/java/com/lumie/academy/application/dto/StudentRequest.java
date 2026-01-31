package com.lumie.academy.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    String password,

    @NotBlank(message = "Name is required")
    String name,

    String phone,

    @NotNull(message = "Academy ID is required")
    Long academyId,

    String studentNumber,
    String grade,
    String schoolName,
    String parentName,
    String parentPhone
) {
}
