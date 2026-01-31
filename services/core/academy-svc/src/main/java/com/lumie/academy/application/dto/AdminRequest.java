package com.lumie.academy.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    String password,

    @NotBlank(message = "Name is required")
    String name,

    String phone,

    Long academyId,

    @NotBlank(message = "Admin type is required")
    String adminType
) {
}
