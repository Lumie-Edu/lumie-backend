package com.lumie.exam.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExamQuestionTypeRequest(
        @Schema(minLength = 1, maxLength = 50)
        @NotBlank(message = "Question type name is required")
        @Size(min = 1, max = 50, message = "Question type name must be 1 to 50 characters")
        String name
) {
}
