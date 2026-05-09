package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.Permission;

public record PermissionResponse(
    Long id,
    String code,
    String name,
    String category,
    String description
) {
    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
            permission.getId(),
            permission.getCode(),
            permission.getName(),
            permission.getCategory(),
            permission.getDescription()
        );
    }
}
