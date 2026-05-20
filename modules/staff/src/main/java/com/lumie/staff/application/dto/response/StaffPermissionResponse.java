package com.lumie.staff.application.dto.response;

import com.lumie.staff.domain.entity.StaffPermission;

public record StaffPermissionResponse(
    String permissionCode,
    String accessLevel
) {
    public static StaffPermissionResponse from(StaffPermission entity) {
        return new StaffPermissionResponse(
            entity.getPermissionCode(),
            entity.getAccessLevel().name()
        );
    }
}
