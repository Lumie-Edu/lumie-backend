package com.lumie.academy.infrastructure.tenant;

/**
 * Holds the current user context for the request thread.
 * User ID is passed from API Gateway via X-User-Id header.
 */
public class UserContextHolder {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

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

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
