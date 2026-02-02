package com.lumie.academy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentRequest(
    @NotBlank(message = "User login ID is required")
    @Size(min = 4, max = 50, message = "User login ID must be between 4 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "User login ID can only contain letters, numbers, and underscores")
    String userLoginId,

    @NotBlank(message = "Name is required")
    String name,

    String phone,

    @NotNull(message = "Academy ID is required")
    Long academyId,

    String studentHighschool,
    Integer studentBirthYear,
    String studentMemo
) {
}
