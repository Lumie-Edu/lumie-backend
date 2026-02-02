package com.lumie.academy.application.dto;

import com.lumie.academy.domain.entity.Academy;

import java.time.LocalDateTime;

public record AcademyResponse(
    Long id,
    String name,
    String address,
    String phone,
    Boolean isActive,
    long studentCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AcademyResponse from(Academy academy, long studentCount) {
        return new AcademyResponse(
            academy.getId(),
            academy.getName(),
            academy.getAddress(),
            academy.getPhone(),
            academy.isActive(),
            studentCount,
            academy.getCreatedAt(),
            academy.getUpdatedAt()
        );
    }

    public static AcademyResponse from(Academy academy) {
        return from(academy, 0);
    }
}
