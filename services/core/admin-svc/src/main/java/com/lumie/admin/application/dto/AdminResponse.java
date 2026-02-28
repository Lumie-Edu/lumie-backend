package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.vo.EmploymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminResponse(
    Long id,
    Long userId,
    String userLoginId,
    String name,
    String phone,
    String email,
    List<Long> academyIds,
    PositionInfo position,
    String adminMemo,
    Boolean isActive,
    EmploymentStatus employmentStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record PositionInfo(Long id, String name) {}

    public static AdminResponse from(Admin admin) {
        PositionInfo positionInfo = admin.getPosition() != null
            ? new PositionInfo(admin.getPosition().getId(), admin.getPosition().getName())
            : null;

        return new AdminResponse(
            admin.getId(),
            admin.getUserId(),
            admin.getUserLoginId(),
            admin.getAdminName(),
            admin.getAdminPhone(),
            admin.getEmail(),
            List.copyOf(admin.getAcademyIds()),
            positionInfo,
            admin.getAdminMemo(),
            admin.isActive(),
            admin.getEmploymentStatus(),
            admin.getCreatedAt(),
            admin.getUpdatedAt()
        );
    }
}
