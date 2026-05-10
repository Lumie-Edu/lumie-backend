package com.lumie.auth.api;

import com.lumie.event.DomainEvent;

/**
 * Domain event published after the OWNER user has been created during owner
 * registration (a brand-new tenant). Other modules listen to bootstrap their
 * own per-OWNER records.
 *
 * <p>Replaces the direct {@code auth.AuthRegistrationService → admin.AdminService.createOwnerAdmin}
 * call that created the auth↔admin circular dependency.
 *
 * <p>Schema version 1.
 */
public record OwnerRegisteredEvent(
        Long userId,
        String userLoginId,
        String name,
        String phone,
        String email,
        String tenantSlug,
        Long tenantId
) implements DomainEvent {
}
