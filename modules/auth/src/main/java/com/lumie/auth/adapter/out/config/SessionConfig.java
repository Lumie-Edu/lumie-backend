package com.lumie.auth.adapter.out.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "session")
public class SessionConfig {

    /** Seven days keeps the existing browser sign-in lifetime. */
    private long expiration = 604800;

    /** Production uses the __Host prefix, which requires Secure + Path=/. */
    private String cookieName = "__Host-lumie_session";

    private boolean cookieSecure = true;

    private String cookieSameSite = "Lax";

    /** Empty in production: only the exact browser origin is accepted. */
    private List<String> csrfAllowedOrigins = List.of();
}
