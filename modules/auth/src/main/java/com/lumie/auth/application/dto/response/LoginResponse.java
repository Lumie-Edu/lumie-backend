package com.lumie.auth.application.dto.response;

import com.lumie.auth.domain.vo.AuthSession;

/** Login result. The opaque ID is written only to the HttpOnly cookie. */
public record LoginResponse(
        String sessionId,
        long sessionExpiresIn,
        UserResponse user
) {
    public static LoginResponse of(AuthSession session, UserResponse user) {
        return new LoginResponse(
                session.id(),
                session.expiresAt().getEpochSecond() - session.createdAt().getEpochSecond(),
                user
        );
    }
}
