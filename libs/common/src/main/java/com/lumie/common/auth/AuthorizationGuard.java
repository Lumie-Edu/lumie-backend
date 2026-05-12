package com.lumie.common.auth;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.UserContextHolder;

public final class AuthorizationGuard {

    private AuthorizationGuard() {
    }

    public static void requireOwner() {
        Role role = UserContextHolder.getUserRole();
        if (role != Role.OWNER) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Owner role required");
        }
    }
}
