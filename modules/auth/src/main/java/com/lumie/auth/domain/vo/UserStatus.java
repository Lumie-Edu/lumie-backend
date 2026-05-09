package com.lumie.auth.domain.vo;

/**
 * Account state for a {@code users} row. Persisted as the underlying enum
 * name via JPA's {@code @Enumerated(EnumType.STRING)}.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE;

    public boolean isEnabled() {
        return this == ACTIVE;
    }
}
