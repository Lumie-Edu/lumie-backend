package com.lumie.staff.application.dto.response;

import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.vo.EmploymentStatus;

import java.time.Instant;

public record StaffResponse(
    Long id,
    Long userId,
    String userLoginId,
    String name,
    String phone,
    String email,
    String role,
    String staffMemo,
    Boolean isActive,
    EmploymentStatus employmentStatus,
    String avatarSeed,
    Instant createdAt,
    Instant updatedAt
) {
    public static StaffResponse from(Staff staff) {
        return from(staff, null);
    }

    public static StaffResponse from(Staff staff, String avatarSeed) {
        return new StaffResponse(
            staff.getId(),
            staff.getUserId(),
            staff.getUserLoginId(),
            staff.getStaffName(),
            staff.getStaffPhone(),
            staff.getEmail(),
            staff.getRole(),
            staff.getStaffMemo(),
            staff.isActive(),
            staff.getEmploymentStatus(),
            avatarSeed,
            staff.getCreatedAt(),
            staff.getUpdatedAt()
        );
    }
}
