package com.lumie.auth.application.dto.response;

import lombok.Builder;

@Builder
public record AuthResponse(
        long accessExpiresIn,
        long refreshExpiresIn,
        UserResponse user
) {
    public static AuthResponse from(LoginResponse loginResponse) {
        return AuthResponse.builder()
                .accessExpiresIn(loginResponse.accessExpiresIn())
                .refreshExpiresIn(loginResponse.refreshExpiresIn())
                .user(loginResponse.user())
                .build();
    }

    public static AuthResponse from(TokenResponse tokenResponse) {
        return AuthResponse.builder()
                .accessExpiresIn(tokenResponse.accessExpiresIn())
                .refreshExpiresIn(tokenResponse.refreshExpiresIn())
                .user(null)
                .build();
    }
}
