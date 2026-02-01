package com.lumie.auth.domain.vo;

/**
 * User role enumeration with hierarchical authority levels.
 * Lower level values have higher authority.
 */
public enum Role {
    OWNER(0),
    DEVELOPER(1),
    ADMIN(2),
    STUDENT(3);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Checks if this role has authority over the required role.
     *
     * @param required the role to check against
     * @return true if this role has equal or higher authority
     */
    public boolean hasAuthority(Role required) {
        return this.level <= required.level;
    }

    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            return STUDENT;
        }
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }
}
