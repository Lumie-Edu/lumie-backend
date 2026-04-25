package com.lumie.content.application.port.out;

import java.util.Optional;

public interface AdminServicePort {

    Optional<AdminData> getAdminByUserId(String tenantSlug, Long userId);

    record AdminData(
            Long id,
            Long userId,
            String adminName,
            Long academyId,
            String academyName,
            boolean isActive
    ) {
    }
}
