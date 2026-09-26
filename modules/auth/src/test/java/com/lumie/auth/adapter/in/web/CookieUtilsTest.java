package com.lumie.auth.adapter.in.web;

import com.lumie.auth.adapter.out.config.SessionConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilsTest {

    @Test
    void productionCookieUsesHostPrefixAndSecureAttributes() {
        String cookie = new CookieUtils(new SessionConfig()).createSessionCookie("session-id").toString();

        assertThat(cookie).contains("__Host-lumie_session=session-id", "Secure", "HttpOnly", "SameSite=Lax", "Path=/");
        assertThat(cookie).doesNotContain("Domain=");
    }

    @Test
    void localDevCookieUsesNonSecureHostOnlyName() {
        SessionConfig config = new SessionConfig();
        config.setCookieName("lumie_session");
        config.setCookieSecure(false);
        String cookie = new CookieUtils(config).createSessionCookie("session-id").toString();

        assertThat(cookie).contains("lumie_session=session-id", "HttpOnly", "SameSite=Lax", "Path=/");
        assertThat(cookie).doesNotContain("Secure", "Domain=");
    }
}
