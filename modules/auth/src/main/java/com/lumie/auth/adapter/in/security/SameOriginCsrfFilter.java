package com.lumie.auth.adapter.in.security;

import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.common.exception.ProblemResponseWriter;
import com.lumie.common.exception.CommonErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

/**
 * Cookie-authenticated state changes must originate from the same host.
 * This complements SameSite=Lax for requests that browsers send with an
 * existing session cookie.
 */
@Component
@RequiredArgsConstructor
public class SameOriginCsrfFilter extends OncePerRequestFilter {

    private final SessionConfig sessionConfig;
    private final SameOriginPolicy sameOriginPolicy;
    private final ProblemResponseWriter problems;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isUnsafeMethod(request) && hasSessionCookie(request) && !sameOriginPolicy.isAllowed(request)) {
            problems.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                    CommonErrorCode.FORBIDDEN.getCode(), "Cross-origin request rejected");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isUnsafeMethod(HttpServletRequest request) {
        String method = request.getMethod();
        return !HttpMethod.GET.matches(method) && !HttpMethod.HEAD.matches(method)
                && !HttpMethod.OPTIONS.matches(method) && !HttpMethod.TRACE.matches(method);
    }

    private boolean hasSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        for (Cookie cookie : cookies) {
            if (sessionConfig.getCookieName().equals(cookie.getName())) return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/internal/");
    }
}
