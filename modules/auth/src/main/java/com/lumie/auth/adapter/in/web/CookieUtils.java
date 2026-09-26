package com.lumie.auth.adapter.in.web;

import com.lumie.auth.adapter.out.config.SessionConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Issues only host-only opaque session cookies. */
@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final SessionConfig sessionConfig;

    public ResponseCookie createSessionCookie(String sessionId) {
        return cookie(sessionId, sessionConfig.getExpiration());
    }

    public ResponseCookie expireSessionCookie() {
        return cookie("", 0);
    }

    private ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(sessionConfig.getCookieName(), value)
                .httpOnly(true)
                .secure(sessionConfig.isCookieSecure())
                .sameSite(sessionConfig.getCookieSameSite())
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
