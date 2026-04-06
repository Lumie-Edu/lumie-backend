package com.lumie.notification.application.port.out;

import java.util.Optional;

public interface AdminServicePort {

    Optional<AdminInfo> getAdmin(Long adminId);

    record AdminInfo(
        Long id,
        String name
    ) {
    }
}
