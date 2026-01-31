package com.lumie.academy.application.dto;

import com.lumie.academy.domain.entity.Academy;

import java.time.LocalDateTime;

public record AcademyResponse(
    Long id,
    String name,
    String description,
    String address,
    String phone,
    String email,
    String businessNumber,
    boolean isDefault,
    String status,
    long studentCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AcademyResponse from(Academy academy, long studentCount) {
        return new AcademyResponse(
            academy.getId(),
            academy.getName(),
            academy.getDescription(),
            academy.getAddress(),
            academy.getPhone(),
            academy.getEmail(),
            academy.getBusinessNumber(),
            academy.isDefault(),
            academy.getStatus(),
            studentCount,
            academy.getCreatedAt(),
            academy.getUpdatedAt()
        );
    }

    public static AcademyResponse from(Academy academy) {
        return from(academy, 0);
    }
}
