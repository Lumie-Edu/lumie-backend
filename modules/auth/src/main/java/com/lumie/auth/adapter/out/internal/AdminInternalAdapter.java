package com.lumie.auth.adapter.out.internal;

import com.lumie.auth.application.port.out.AdminServicePort;
import com.lumie.internal.admin.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("authAdminInternalAdapter")
@RequiredArgsConstructor
public class AdminInternalAdapter implements AdminServicePort {

    private final AdminService adminService;

    @Override
    public void createOwnerAdmin(String tenantSlug, Long userId, String userLoginId,
                                 String name, String phone, String email) {
        log.debug("Creating OWNER admin via internal API: tenant={}, userId={}", tenantSlug, userId);
        adminService.createOwnerAdmin(tenantSlug, userId, userLoginId, name, phone, email);
    }
}
