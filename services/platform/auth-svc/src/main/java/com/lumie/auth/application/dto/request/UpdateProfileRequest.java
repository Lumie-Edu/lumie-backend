package com.lumie.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email
) {
}
