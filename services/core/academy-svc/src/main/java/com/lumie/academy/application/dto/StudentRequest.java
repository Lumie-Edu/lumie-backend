package com.lumie.academy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentRequest(
    @NotBlank(message = "Name is required")
    String name,

    String phone,

    @NotNull(message = "Academy ID is required")
    Long academyId,

    String studentHighschool,
    Integer studentBirthYear,
    String parentPhone,
    String studentMemo
) {
}
