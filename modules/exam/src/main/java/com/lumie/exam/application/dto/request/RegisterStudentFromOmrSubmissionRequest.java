package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterStudentFromOmrSubmissionRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @Pattern(regexp = "^$|^[0-9-]{11,13}$")
        String phone,
        @Size(max = 100)
        String studentHighschool,
        @Min(1900) @Max(2100)
        Integer studentBirthYear,
        @Pattern(regexp = "^$|^[0-9-]{10,13}$")
        String parentPhone,
        @Size(max = 500)
        String studentMemo
) {
}
