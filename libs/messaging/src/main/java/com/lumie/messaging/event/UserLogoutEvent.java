package com.lumie.messaging.event;

import lombok.Getter;

@Getter
public class UserLogoutEvent extends AuthEvent {

    private final boolean logoutAll;

    public UserLogoutEvent(Long userId, String tenantSlug, boolean logoutAll) {
        super(userId, tenantSlug);
        this.logoutAll = logoutAll;
    }

    @Override
    public String getRoutingKey() {
        return String.format("auth.logout.%s", getTenantSlug());
    }
}
