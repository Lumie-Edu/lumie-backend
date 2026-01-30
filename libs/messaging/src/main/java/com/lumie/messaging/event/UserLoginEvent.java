package com.lumie.messaging.event;

import lombok.Getter;

@Getter
public class UserLoginEvent extends AuthEvent {

    private final String email;
    private final String provider;

    public UserLoginEvent(Long userId, String tenantSlug, String email, String provider) {
        super(userId, tenantSlug);
        this.email = email;
        this.provider = provider;
    }

    @Override
    public String getRoutingKey() {
        return String.format("auth.login.%s", getTenantSlug());
    }
}
