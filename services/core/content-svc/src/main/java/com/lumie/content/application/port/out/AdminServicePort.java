package com.lumie.content.application.port.out;

import java.util.Optional;

public interface AdminServicePort {

    boolean validateAdmin(String tenantSlug, Long adminId);

    Optional<AdminData> getAdmin(String tenantSlug, Long adminId);

    record AdminData(
            Long id,
            Long userId,
            String adminName,
            Long academyId,
            String academyName,
            String position,
            boolean isActive
    ) {
    }
}
