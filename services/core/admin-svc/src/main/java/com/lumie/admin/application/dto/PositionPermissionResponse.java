package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.PositionPermission;

public record PositionPermissionResponse(
    String permissionCode,
    String accessLevel
) {
    public static PositionPermissionResponse from(PositionPermission entity) {
        return new PositionPermissionResponse(
            entity.getPermissionCode(),
            entity.getAccessLevel().name()
        );
    }
}
