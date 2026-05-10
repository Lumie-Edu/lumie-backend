package com.lumie.admin.application.dto;

import com.lumie.admin.domain.vo.AccessLevel;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request body for {@code PUT /v1/admins/{adminId}/permissions}.
 *
 * <p>Each entry maps a permission code (e.g. {@code STUDENT_WRITE}) to the
 * {@link AccessLevel} granted. Replaces a former {@code Map<String, String>}
 * body which lost validation and AccessLevel parsing safety.
 */
public record SetPermissionsRequest(
        @NotNull(message = "permissions map is required")
        Map<String, AccessLevel> permissions
) {
}
