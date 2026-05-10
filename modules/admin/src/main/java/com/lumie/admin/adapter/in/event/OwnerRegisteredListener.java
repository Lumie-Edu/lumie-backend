package com.lumie.admin.adapter.in.event;

import com.lumie.admin.api.AdminService;
import com.lumie.auth.api.OwnerRegisteredEvent;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link OwnerRegisteredEvent} after owner-registration commit
 * and bootstraps the matching OWNER admin record in the tenant schema.
 *
 * <p>Replaces the direct {@code auth.AuthRegistrationService → admin.AdminService.createOwnerAdmin}
 * call that created the auth↔admin circular dependency
 * (see {@code temp/audits-cleanup/CIRCULAR_MODULE_DEPS_AUDIT.md}, Resolution A pair 4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerRegisteredListener {

    private final AdminService adminService;

    @ApplicationModuleListener
    public void onOwnerRegistered(OwnerRegisteredEvent event) {
        Runnable createOwner = () -> {
            try {
                adminService.createOwnerAdmin(
                        event.tenantSlug(), event.userId(), event.userLoginId(),
                        event.name(), event.phone(), event.email());
                log.info("OWNER admin bootstrapped for tenant {}: userId={}",
                        event.tenantSlug(), event.userId());
            } catch (Exception e) {
                // Bootstrap failure leaves the OWNER user without an admin record;
                // surfaces as a manual recovery path rather than rolling back the
                // already-committed tenant + user creation.
                log.error("Failed to bootstrap OWNER admin for tenant {} (userId={}): {}",
                        event.tenantSlug(), event.userId(), e.getMessage(), e);
            }
        };
        if (TenantContextHolder.getTenant() == null) {
            TenantContextHolder.withinContext(event.tenantSlug(), event.tenantId(), () -> {
                createOwner.run();
                return null;
            });
        } else {
            createOwner.run();
        }
    }
}
