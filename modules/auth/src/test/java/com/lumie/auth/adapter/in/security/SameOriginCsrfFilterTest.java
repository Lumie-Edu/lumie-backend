package com.lumie.auth.adapter.in.security;

import com.lumie.auth.adapter.out.config.SessionConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SameOriginCsrfFilterTest {

    @Test
    void productionRejectsAProxyOriginThatDoesNotMatchBackendHost() throws Exception {
        MockHttpServletRequest request = sessionRequest();
        request.addHeader("Origin", "http://localhost:3000");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SameOriginCsrfFilter(new SessionConfig(), new SameOriginPolicy(new SessionConfig()), new com.lumie.common.exception.ProblemResponseWriter(new com.fasterxml.jackson.databind.ObjectMapper())).doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void devAllowsOnlyConfiguredLocalBrowserOrigin() throws Exception {
        SessionConfig config = new SessionConfig();
        config.setCookieName("lumie_session");
        config.setCsrfAllowedOrigins(List.of("http://localhost:3000"));
        MockHttpServletRequest request = sessionRequest();
        request.setCookies(new Cookie("lumie_session", "session-id"));
        request.addHeader("Origin", "http://localhost:3000");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new SameOriginCsrfFilter(config, new SameOriginPolicy(config), new com.lumie.common.exception.ProblemResponseWriter(new com.fasterxml.jackson.databind.ObjectMapper())).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void sharedPolicyRejectsBlankOriginAndUsesFallbackHostAndScheme() {
        var policy = new SameOriginPolicy(new SessionConfig());
        var request = new MockHttpServletRequest();
        request.setServerName("example.com");
        request.setScheme("https");
        request.addHeader("Origin", "https://example.com");
        request.addHeader("X-Forwarded-Proto", "");
        assertThat(policy.isAllowed(request)).isTrue();
        request.removeHeader("Origin");
        request.addHeader("Origin", " ");
        assertThat(policy.isAllowed(request)).isFalse();
    }

    private MockHttpServletRequest sessionRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/logout");
        request.setServerName("dev.lumie-infra.com");
        request.addHeader("Host", "dev.lumie-infra.com");
        request.setCookies(new Cookie("__Host-lumie_session", "session-id"));
        return request;
    }
}
