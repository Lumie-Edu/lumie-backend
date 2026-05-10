package com.lumie.admin.api;

import java.util.List;
import java.util.Optional;

public interface AdminService {

    Optional<AdminData> getAdmin(String tenantSlug, Long adminId);

    Optional<AdminData> getAdminByUserId(String tenantSlug, Long userId);

    ValidateAdminResult validateAdmin(String tenantSlug, Long adminId);

    AdminPermissionsData getAdminPermissions(String tenantSlug, Long adminId);

    AdminListData getAllAdmins(String tenantSlug, int page, int size);

    AdminData createOwnerAdmin(String tenantSlug, Long userId, String userLoginId, String name, String phone, String email);

    record AdminData(
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

    record ValidateAdminResult(
            boolean valid,
            String message,
            Long adminId
    ) {}

    record AdminPermissionsData(
            Long adminId,
            List<PermissionEntry> permissions
    ) {}

    record PermissionEntry(
            String permissionCode,
            String accessLevel
    ) {}

    record AdminListData(
            List<AdminData> admins,
            long totalCount,
            int page,
            int size
    ) {}
}
