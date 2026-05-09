package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.AdminPermission;

public record AdminPermissionResponse(
    String permissionCode,
    String accessLevel
) {
    public static AdminPermissionResponse from(AdminPermission entity) {
        return new AdminPermissionResponse(
            entity.getPermissionCode(),
            entity.getAccessLevel().name()
        );
    }
}
