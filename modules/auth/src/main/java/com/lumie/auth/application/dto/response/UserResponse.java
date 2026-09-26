package com.lumie.auth.application.dto.response;

import com.lumie.common.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UserResponse(
        Long id,
        String userLoginId,
        String name,
        String phone,
        String email,
        Role role,
        String tenantSlug,
        Long tenantId,
        @Schema(nullable = true)
        Instant tenantOnboardingCompletedAt,
        String avatarSeed
) {
}
