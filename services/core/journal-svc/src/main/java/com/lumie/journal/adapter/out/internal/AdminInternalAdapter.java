package com.lumie.journal.adapter.out.internal;

import com.lumie.journal.application.port.out.AdminServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.admin.AdminInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("journalAdminInternalAdapter")
@RequiredArgsConstructor
public class AdminInternalAdapter implements AdminServicePort {

    private final AdminInternalService adminInternalService;

    @Override
    public Optional<AdminInfo> getAdmin(Long adminId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting admin via internal API: {} in tenant: {}", adminId, slug);
        return adminInternalService.getAdmin(slug, adminId)
                .map(a -> new AdminInfo(a.id(), a.name()));
    }
}
