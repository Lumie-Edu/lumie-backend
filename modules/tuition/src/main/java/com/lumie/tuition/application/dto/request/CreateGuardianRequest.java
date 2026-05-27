package com.lumie.tuition.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateGuardianRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String email,
        String relationship
) {}
