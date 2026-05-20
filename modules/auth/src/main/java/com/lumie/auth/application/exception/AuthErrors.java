package com.lumie.auth.application.exception;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;

/**
 * Application-layer exceptions for the auth flows. Grouped here as nested
 * classes to keep import surfaces tidy and maintain a single ownership point
 * for auth-specific failure semantics.
 */
public final class AuthErrors {

    private AuthErrors() {}

    /** Wrong login id / password, expired account, etc. — maps to 401. */
    public static class InvalidCredentialsException extends BusinessException {
        public InvalidCredentialsException(String message) {
            super(CommonErrorCode.UNAUTHORIZED, message);
        }
    }

    /** Malformed / blacklisted / expired refresh token — maps to 401. */
    public static class InvalidTokenException extends BusinessException {
        public InvalidTokenException(String message) {
            super(CommonErrorCode.UNAUTHORIZED, message);
        }
    }

    /** Login id already in use during registration — maps to 409. */
    public static class UserLoginIdAlreadyExistsException extends BusinessException {
        public UserLoginIdAlreadyExistsException(String message) {
            super(AuthErrorCode.LOGIN_ID_ALREADY_EXISTS, message);
        }
    }

    /** Owner registration could not provision a new tenant — maps to 500. */
    public static class TenantCreationFailedException extends BusinessException {
        public TenantCreationFailedException(String message) {
            super(AuthErrorCode.TENANT_CREATION_FAILED, message);
        }
    }

    /** Login ID generation exhausted all retry attempts — maps to 500. */
    public static class LoginIdGenerationFailedException extends BusinessException {
        public LoginIdGenerationFailedException() {
            super(AuthErrorCode.LOGIN_ID_GENERATION_FAILED);
        }
    }
}
