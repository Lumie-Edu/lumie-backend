package com.lumie.messaging.event;

import com.lumie.common.domain.AbstractDomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class BillingEvent extends AbstractDomainEvent {

    private final Long tenantId;
    private final String tenantSlug;

    protected BillingEvent(Long tenantId, String tenantSlug) {
        super();
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
    }

    public String getRoutingKey() {
        return String.format("billing.%s.%s",
                getEventType().toLowerCase().replace("event", ""),
                tenantSlug);
    }
}
