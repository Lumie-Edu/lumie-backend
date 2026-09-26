package com.lumie.auth.adapter.in.security;

import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.RequestContextFilter;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationFilterTest {

    @Mock private AuthQueryService authQueryService;

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    void rejectsSessionUsedOnAnotherTenantHost() throws Exception {
        when(authQueryService.findActiveSession("session-id")).thenReturn(Optional.of(session("inst-academy")));
        MockHttpServletRequest request = request();
        request.setAttribute(RequestContextFilter.HOST_TENANT_SLUG_ATTRIBUTE, "inst-other");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        new SessionAuthenticationFilter(authQueryService, sessionConfig()).doFilter(request, response,
                (ignoredRequest, ignoredResponse) -> chainCalled.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chainCalled).isFalse();
    }

    @Test
    void bindsSessionTenantAndUserToSecurityContext() throws Exception {
        when(authQueryService.findActiveSession("session-id")).thenReturn(Optional.of(session("inst-academy")));
        MockHttpServletRequest request = request();
        request.setAttribute(RequestContextFilter.HOST_TENANT_SLUG_ATTRIBUTE, "inst-academy");

        new SessionAuthenticationFilter(authQueryService, sessionConfig()).doFilter(request, new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> { });

        assertThat(UserContextHolder.getUserId()).isEqualTo(20L);
        assertThat(TenantContextHolder.getTenant()).isEqualTo("inst-academy");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_STUDENT");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/me");
        request.setCookies(new Cookie(sessionConfig().getCookieName(), "session-id"));
        return request;
    }

    private AuthSession session(String tenantSlug) {
        Instant now = Instant.now();
        return new AuthSession("session-id", 20L, "Student", tenantSlug, 10L, Role.STUDENT,
                "Chrome / macOS", DeviceCategory.PC, now, now.plusSeconds(3600));
    }

    private SessionConfig sessionConfig() {
        return new SessionConfig();
    }
}
