package com.lumie.admin.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PositionPermissionRequest(
    @NotNull(message = "Permissions are required")
    Map<String, String> permissions
) {
}
