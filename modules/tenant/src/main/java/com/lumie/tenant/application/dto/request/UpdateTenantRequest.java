package com.lumie.tenant.application.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateTenantRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 200, message = "Display name must be at most 200 characters")
        String displayName,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Pattern(
                regexp = "^$|^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$",
                message = "학원 ID는 3-30자의 소문자 영숫자와 하이픈만 사용할 수 있습니다."
        )
        String customId
) {
}
