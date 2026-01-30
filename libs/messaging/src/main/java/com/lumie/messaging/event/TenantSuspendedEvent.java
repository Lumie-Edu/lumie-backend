package com.lumie.messaging.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class TenantSuspendedEvent extends TenantEvent {

    private final String reason;

    public TenantSuspendedEvent(Long tenantId, String slug, String reason) {
        super(tenantId, slug);
        this.reason = reason;
    }

    @Override
    public String getRoutingKey() {
        return String.format("tenant.suspended.%s", getSlug());
    }
}
