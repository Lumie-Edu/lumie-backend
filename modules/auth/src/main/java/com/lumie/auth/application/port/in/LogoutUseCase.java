package com.lumie.auth.application.port.in;

public interface LogoutUseCase {
    /**
     * Logs out a user by blacklisting their token.
     *
     * @param token the access token to invalidate
     */
    void logout(String token);

    /**
     * Logs out a user from all devices by invalidating all refresh tokens.
     *
     * @param token the access token to identify the user
     */
    void logoutAll(String token);
}
