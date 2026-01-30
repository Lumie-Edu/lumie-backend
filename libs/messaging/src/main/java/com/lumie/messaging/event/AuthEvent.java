package com.lumie.messaging.event;

import com.lumie.common.domain.AbstractDomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AuthEvent extends AbstractDomainEvent {

    private final Long userId;
    private final String tenantSlug;

    protected AuthEvent(Long userId, String tenantSlug) {
        super();
        this.userId = userId;
        this.tenantSlug = tenantSlug;
    }

    public String getRoutingKey() {
        return String.format("auth.%s.%s", getEventType().toLowerCase().replace("event", ""), tenantSlug);
    }
}
