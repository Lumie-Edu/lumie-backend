package com.lumie.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lumie.common.domain.AbstractDomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class TenantEvent extends AbstractDomainEvent {

    private final Long tenantId;
    private final String slug;

    protected TenantEvent(Long tenantId, String slug) {
        super();
        this.tenantId = tenantId;
        this.slug = slug;
    }

    @JsonIgnore
    public String getRoutingKey() {
        return String.format("tenant.%s.%s", getEventType().toLowerCase().replace("event", ""), slug);
    }
}
