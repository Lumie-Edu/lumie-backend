package com.lumie.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterRequest(
        @NotBlank(message = "Tenant slug is required")
        String tenantSlug,

        @NotBlank(message = "User login ID is required")
        @Size(min = 4, max = 50, message = "User login ID must be between 4 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "User login ID can only contain letters, numbers, and underscores")
        String userLoginId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
        )
        String password,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone
) {
}
