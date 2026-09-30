package com.lumie.staff.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import com.lumie.common.validation.PersonName;
import com.lumie.common.validation.PersonNamePolicy;
import com.lumie.common.validation.MobilePhone;

public record StaffRequest(
    @NotBlank(message = "Role is required")
    String role,

    @PersonName
        @Schema(minLength = PersonNamePolicy.MIN_LENGTH, maxLength = PersonNamePolicy.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @MobilePhone
    String phone,
    @Email
    @Size(max = 255)
    String email,

    String staffMemo
) {
}
