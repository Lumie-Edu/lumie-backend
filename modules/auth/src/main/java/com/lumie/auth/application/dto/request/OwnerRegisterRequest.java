package com.lumie.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import com.lumie.common.validation.PersonName;
import com.lumie.common.validation.PersonNamePolicy;
import com.lumie.common.validation.MobilePhone;
import com.lumie.common.validation.LoginId;
import com.lumie.common.validation.LoginIdPolicy;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record OwnerRegisterRequest(
        @LoginId
        @Schema(minLength = LoginIdPolicy.MIN_LENGTH, maxLength = LoginIdPolicy.MAX_LENGTH,
                pattern = LoginIdPolicy.PATTERN, requiredMode = Schema.RequiredMode.REQUIRED)
        String userLoginId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
        )
        String password,

        @PersonName
        @Schema(minLength = PersonNamePolicy.MIN_LENGTH, maxLength = PersonNamePolicy.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @MobilePhone
        String phone,

        @NotBlank(message = "Institute name is required")
        @Size(min = 2, max = 200, message = "Institute name must be between 2 and 200 characters")
        String instituteName
) {
}
