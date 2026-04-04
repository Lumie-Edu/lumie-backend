package com.lumie.content.adapter.out.internal;

import com.lumie.content.application.port.out.AdminServicePort;
import com.lumie.internal.admin.AdminInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInternalAdapter implements AdminServicePort {

    private final AdminInternalService adminInternalService;

    @Override
    public boolean validateAdmin(String tenantSlug, Long adminId) {
        log.debug("Validating admin via internal API: {} in tenant: {}", adminId, tenantSlug);
        return adminInternalService.validateAdmin(tenantSlug, adminId).valid();
    }

    @Override
    public Optional<AdminData> getAdmin(String tenantSlug, Long adminId) {
        log.debug("Getting admin via internal API: {} in tenant: {}", adminId, tenantSlug);
        return adminInternalService.getAdmin(tenantSlug, adminId)
                .map(a -> new AdminData(
                        a.id(),
                        a.userId(),
                        a.name(),
                        null,
                        null,
                        a.positionName(),
                        a.isActive()
                ));
    }
}
