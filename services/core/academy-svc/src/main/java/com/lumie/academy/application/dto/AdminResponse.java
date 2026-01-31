package com.lumie.academy.application.dto;

import com.lumie.academy.domain.entity.Admin;

import java.time.LocalDateTime;

public record AdminResponse(
    Long id,
    Long userId,
    String email,
    String name,
    String phone,
    Long academyId,
    String academyName,
    String adminType,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AdminResponse from(Admin admin) {
        return new AdminResponse(
            admin.getId(),
            admin.getUserId(),
            admin.getAdminEmail(),
            admin.getAdminName(),
            admin.getAdminPhone(),
            admin.getAcademy() != null ? admin.getAcademy().getId() : null,
            admin.getAcademy() != null ? admin.getAcademy().getName() : null,
            admin.getAdminType(),
            admin.isActive() ? "ACTIVE" : "INACTIVE",
            admin.getCreatedAt(),
            admin.getUpdatedAt()
        );
    }
}
