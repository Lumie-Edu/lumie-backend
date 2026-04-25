package com.lumie.content.adapter.out.internal;

import com.lumie.content.application.port.out.AdminServicePort;
import com.lumie.internal.admin.AdminInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("contentAdminInternalAdapter")
@RequiredArgsConstructor
public class AdminInternalAdapter implements AdminServicePort {

    private final AdminInternalService adminInternalService;

    @Override
    public Optional<AdminData> getAdminByUserId(String tenantSlug, Long userId) {
        log.debug("Getting admin by userId via internal API: {} in tenant: {}", userId, tenantSlug);
        return adminInternalService.getAdminByUserId(tenantSlug, userId)
                .map(a -> new AdminData(
                        a.id(),
                        a.userId(),
                        a.name(),
                        null,
                        null,
                        a.isActive()
                ));
    }
}
