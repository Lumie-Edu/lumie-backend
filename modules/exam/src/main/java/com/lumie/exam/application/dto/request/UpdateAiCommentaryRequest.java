package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateAiCommentaryRequest(
        @NotBlank @Size(max = 10_000) String content,
        @NotNull @PositiveOrZero Long expectedVersion
) {
}
