package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateTextbookRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        String author,

        String publisher,

        String isbn,

        String subject,

        String gradeLevel,

        BigDecimal price,

        String coverImagePath
) {
}
