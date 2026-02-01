package com.lumie.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record OwnerRegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

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

        @NotBlank(message = "Institute name is required")
        @Size(min = 2, max = 200, message = "Institute name must be between 2 and 200 characters")
        String instituteName,

        @NotBlank(message = "Business registration number is required")
        @Pattern(
                regexp = "^\\d{3}-\\d{2}-\\d{5}$",
                message = "Business registration number must be in format XXX-XX-XXXXX"
        )
        String businessRegistrationNumber
) {
}
