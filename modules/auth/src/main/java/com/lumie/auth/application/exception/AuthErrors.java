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

    /** Supplied current password does not match the stored hash — maps to 401. */
    public static class CurrentPasswordIncorrectException extends BusinessException {
        public CurrentPasswordIncorrectException(String message) {
            super(AuthErrorCode.CURRENT_PASSWORD_INCORRECT, message);
        }
    }

    /** New password is identical to the current password — maps to 422. */
    public static class PasswordUnchangedException extends BusinessException {
        public PasswordUnchangedException(String message) {
            super(AuthErrorCode.PASSWORD_UNCHANGED, message);
        }
    }

    /** User account has been disabled by an administrator — maps to 403. */
    public static class UserDisabledException extends BusinessException {
        public UserDisabledException(String message) {
            super(AuthErrorCode.USER_DISABLED, message);
        }
    }

    /** Tenant is not in an active state for this operation — maps to 403. */
    public static class TenantInactiveException extends BusinessException {
        public TenantInactiveException(String message) {
            super(AuthErrorCode.TENANT_INACTIVE, message);
        }
    }

}
