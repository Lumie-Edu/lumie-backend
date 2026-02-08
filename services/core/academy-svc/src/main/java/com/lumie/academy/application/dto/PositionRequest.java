package com.lumie.academy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PositionRequest(
    @NotBlank(message = "Position name is required")
    @Size(max = 50, message = "Position name must be at most 50 characters")
    String name
) {
}
