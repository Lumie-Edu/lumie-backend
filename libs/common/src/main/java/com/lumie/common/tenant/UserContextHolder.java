package com.lumie.common.tenant;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;

/**
 * Holds the current user context for the request thread.
 * Populated by the authenticated browser session or an internal request.
 */
public final class UserContextHolder {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_NAME = new ThreadLocal<>();
    private static final ThreadLocal<Role> CURRENT_USER_ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SID = new ThreadLocal<>();

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
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "User ID is not set in context");
        }
        return userId;
    }

    /** Display name captured when the current server-side session was created. */
    public static void setUserName(String name) {
        CURRENT_USER_NAME.set(name);
    }

    public static String getUserName() {
        return CURRENT_USER_NAME.get();
    }

    /** Store the canonical role from the authenticated session. */
    public static void setUserRole(Role role) {
        CURRENT_USER_ROLE.set(role);
    }

    public static Role getUserRole() {
        return CURRENT_USER_ROLE.get();
    }

    public static void setCurrentSid(String sid) {
        CURRENT_SID.set(sid);
    }

    public static String getCurrentSid() {
        return CURRENT_SID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_NAME.remove();
        CURRENT_USER_ROLE.remove();
        CURRENT_SID.remove();
    }
}
