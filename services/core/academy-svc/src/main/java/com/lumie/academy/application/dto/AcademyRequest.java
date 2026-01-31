package com.lumie.academy.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AcademyRequest(
    @NotBlank(message = "Name is required")
    String name,

    String description,
    String address,
    String phone,
    String email,
    String businessNumber
) {
}
