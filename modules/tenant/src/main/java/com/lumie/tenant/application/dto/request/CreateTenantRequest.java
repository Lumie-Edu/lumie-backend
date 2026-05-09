package com.lumie.tenant.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateTenantRequest(
        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z][a-z0-9-]{2,29}$", message = "Slug must start with a letter, contain only lowercase letters, numbers, and hyphens, and be 3-30 characters")
        String slug,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 200, message = "Display name must be at most 200 characters")
        String displayName,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String ownerEmail
) {
}
