package com.lumie.common.tenant;

/**
 * Holds the current user context for the request thread.
 * User information is extracted from headers set by Kong JWT plugin:
 * - X-User-Id: User ID from JWT sub claim
 * - X-User-Role: User role from JWT role claim
 */
public final class UserContextHolder {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_ROLE = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static Long getRequiredUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("User ID is not set in context");
        }
        return userId;
    }

    public static void setUserRole(String role) {
        CURRENT_USER_ROLE.set(role);
    }

    public static String getUserRole() {
        return CURRENT_USER_ROLE.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_ROLE.remove();
    }
}
