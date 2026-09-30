package com.lumie.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import io.swagger.v3.oas.annotations.media.Schema;
import com.lumie.common.validation.PersonName;
import com.lumie.common.validation.PersonNamePolicy;
import com.lumie.common.validation.MobilePhone;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @PersonName
        @Schema(minLength = PersonNamePolicy.MIN_LENGTH, maxLength = PersonNamePolicy.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @MobilePhone
        String phone,

        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email
) {
}
