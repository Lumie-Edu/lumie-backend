package com.lumie.staff.api;

import java.util.List;
import java.util.Optional;

public interface StaffService {

    Optional<StaffData> getStaff(String tenantSlug, Long staffId);

    Optional<StaffData> getStaffByUserId(String tenantSlug, Long userId);

    ValidateStaffResult validateStaff(String tenantSlug, Long staffId);

    StaffPermissionsData getStaffPermissions(String tenantSlug, Long staffId);

    StaffListData getAllStaff(String tenantSlug, int page, int size);

    StaffData createOwnerStaff(String tenantSlug, Long userId, String userLoginId, String name, String phone, String email);

    record StaffData(
            Long id,
            Long userId,
            String userLoginId,
            String name,
            String phone,
            String email,
            boolean isActive,
            String employmentStatus,
            String createdAt,
            String updatedAt
    ) {}

    record ValidateStaffResult(
            boolean valid,
            String message,
            Long staffId
    ) {}

    record StaffPermissionsData(
            Long staffId,
            List<PermissionEntry> permissions
    ) {}

    record PermissionEntry(
            String permissionCode,
            String accessLevel
    ) {}

    record StaffListData(
            List<StaffData> staff,
            long totalCount,
            int page,
            int size
    ) {}
}
