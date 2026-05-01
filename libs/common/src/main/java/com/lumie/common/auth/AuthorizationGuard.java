package com.lumie.common.auth;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.UserContextHolder;

public final class AuthorizationGuard {

    private static final String OWNER_ROLE = "OWNER";

    private AuthorizationGuard() {
    }

    public static void requireOwner() {
        String role = UserContextHolder.getUserRole();
        if (!OWNER_ROLE.equalsIgnoreCase(role)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Owner role required");
        }
    }
}
