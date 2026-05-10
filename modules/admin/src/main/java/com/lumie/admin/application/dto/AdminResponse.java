package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.vo.EmploymentStatus;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

public record AdminResponse(
    Long id,
    Long userId,
    String userLoginId,
    String name,
    String phone,
    String email,
    String role,
    String adminMemo,
    Boolean isActive,
    EmploymentStatus employmentStatus,
    String avatarSeed,
    Instant createdAt,
    Instant updatedAt
) {
    public static AdminResponse from(Admin admin) {
        return from(admin, null);
    }

    public static AdminResponse from(Admin admin, String avatarSeed) {
        return new AdminResponse(
            admin.getId(),
            admin.getUserId(),
            admin.getUserLoginId(),
            admin.getAdminName(),
            admin.getAdminPhone(),
            admin.getEmail(),
            admin.getRole(),
            admin.getAdminMemo(),
            admin.isActive(),
            admin.getEmploymentStatus(),
            avatarSeed,
            Timestamps.utcInstant(admin.getCreatedAt()),
            Timestamps.utcInstant(admin.getUpdatedAt())
        );
    }
}
