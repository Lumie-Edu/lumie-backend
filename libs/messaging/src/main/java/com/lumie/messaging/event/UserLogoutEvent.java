package com.lumie.messaging.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
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
