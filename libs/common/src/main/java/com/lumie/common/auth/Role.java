package com.lumie.common.auth;

import java.util.Locale;

/**
 * User role with hierarchical authority.
 *
 * <p>Lower {@link #level} = higher authority. {@link #hasAuthority(Role)}
 * checks "this role >= required" (e.g. OWNER hasAuthority over MANAGER).
 * {@link #canManage(Role)} checks "this role > target" — used by admin
 * lifecycle ops where an actor must strictly outrank the target (e.g. an
 * INSTRUCTOR cannot deactivate another INSTRUCTOR).
 *
 * <p>Lives in {@code libs/common} so any module can use it without
 * depending on auth-svc.
 */
public enum Role {
    OWNER(0),
    MANAGER(1),
    INSTRUCTOR(2),
    STUDENT(3);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * @return true if this role has equal-or-higher authority than {@code required}
     */
    public boolean hasAuthority(Role required) {
        return this.level <= required.level;
    }

    /**
     * @return true if this role strictly outranks {@code target} (used to gate
     *         admin lifecycle ops where peers cannot manage peers)
     */
    public boolean canManage(Role target) {
        return this.level < target.level;
    }

    /**
     * Lenient parse from string. Returns {@link #STUDENT} for null / blank /
     * unknown values (preserving the legacy behavior). Honors the historical
     * aliases {@code ADMIN -> INSTRUCTOR} and {@code DEVELOPER -> MANAGER}.
     */
    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            return STUDENT;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return switch (normalized) {
                case "ADMIN" -> INSTRUCTOR;
                case "DEVELOPER" -> MANAGER;
                default -> STUDENT;
            };
        }
    }
}
