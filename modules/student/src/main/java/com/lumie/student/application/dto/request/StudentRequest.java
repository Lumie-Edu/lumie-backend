package com.lumie.student.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StudentRequest(
    @NotBlank(message = "Name is required")
    String name,

    String phone,

    String studentHighschool,
    Integer studentBirthYear,
    String parentPhone,
    String studentMemo
) {
}
