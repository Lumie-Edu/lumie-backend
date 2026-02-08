package com.lumie.attendance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CheckInRequest(
    @NotBlank(message = "Attendance code is required")
    @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
    String code
) {
}
