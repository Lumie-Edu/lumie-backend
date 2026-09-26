package com.lumie.auth.application.dto.response;

/** Browser authentication response; credentials remain exclusively in the cookie. */
public record AuthResponse(long sessionExpiresIn, UserResponse user) {
    public static AuthResponse from(LoginResponse response) {
        return new AuthResponse(response.sessionExpiresIn(), response.user());
    }
}
