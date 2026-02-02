package com.lumie.academy.application.dto;

import com.lumie.academy.domain.entity.Admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminResponse(
    Long id,
    Long userId,
    String userLoginId,
    String name,
    String phone,
    List<AcademyInfo> academies,
    String adminPosition,
    String adminMemo,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record AcademyInfo(Long id, String name) {}

    public static AdminResponse from(Admin admin) {
        List<AcademyInfo> academyInfos = admin.getAcademies().stream()
            .map(a -> new AcademyInfo(a.getId(), a.getName()))
            .toList();

        return new AdminResponse(
            admin.getId(),
            admin.getUserId(),
            admin.getUserLoginId(),
            admin.getAdminName(),
            admin.getAdminPhone(),
            academyInfos,
            admin.getAdminPosition(),
            admin.getAdminMemo(),
            admin.isActive(),
            admin.getCreatedAt(),
            admin.getUpdatedAt()
        );
    }
}
