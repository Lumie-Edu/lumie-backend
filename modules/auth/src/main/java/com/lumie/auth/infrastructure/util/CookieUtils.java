package com.lumie.auth.infrastructure.util;

import com.lumie.auth.infrastructure.config.CookieConfig;
import com.lumie.auth.infrastructure.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final CookieConfig cookieConfig;
    private final JwtConfig jwtConfig;

    public ResponseCookie createAccessTokenCookie(String token) {
        return buildCookie(cookieConfig.getAccessTokenName(), token, jwtConfig.getAccessExpiration());
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return buildCookie(cookieConfig.getRefreshTokenName(), token, jwtConfig.getRefreshExpiration());
    }

    public ResponseCookie createExpiredAccessTokenCookie() {
        return buildCookie(cookieConfig.getAccessTokenName(), "", 0);
    }

    public ResponseCookie createExpiredRefreshTokenCookie() {
        return buildCookie(cookieConfig.getRefreshTokenName(), "", 0);
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .path(cookieConfig.getPath())
                .maxAge(maxAgeSeconds)
                .sameSite(cookieConfig.getSameSite());

        if (cookieConfig.getDomain() != null && !cookieConfig.getDomain().isEmpty()) {
            builder.domain(cookieConfig.getDomain());
        }

        return builder.build();
    }
}
