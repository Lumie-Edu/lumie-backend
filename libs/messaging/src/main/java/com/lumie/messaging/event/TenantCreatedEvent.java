package com.lumie.messaging.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class TenantCreatedEvent extends TenantEvent {

    private final String tenantName;

    public TenantCreatedEvent(Long tenantId, String slug, String tenantName) {
        super(tenantId, slug);
        this.tenantName = tenantName;
    }

    @Override
    public String getRoutingKey() {
        return String.format("tenant.created.%s", getSlug());
    }
}
