package com.lumie.auth.application.dto.response;

import com.lumie.auth.domain.vo.Role;
import lombok.Builder;

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
        String avatarSeed
) {
}
