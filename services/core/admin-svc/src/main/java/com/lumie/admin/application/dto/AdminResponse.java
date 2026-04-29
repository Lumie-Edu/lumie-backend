package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.vo.EmploymentStatus;

import java.time.LocalDateTime;

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
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AdminResponse from(Admin admin) {
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
            admin.getCreatedAt(),
            admin.getUpdatedAt()
        );
    }
}
