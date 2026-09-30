package com.lumie.auth.adapter.in.security;

import com.lumie.auth.adapter.out.config.SessionConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SameOriginPolicy {
    private final SessionConfig sessionConfig;

    public boolean isAllowed(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) return false;
        if (sessionConfig.getCsrfAllowedOrigins().contains(origin)) return true;
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) scheme = request.getScheme();
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) host = request.getServerName();
        return (scheme + "://" + host).equalsIgnoreCase(origin);
    }

}
