package com.lumie.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secretKey;

    private long accessExpiration = 3600; // 1 hour in seconds

    private long refreshExpiration = 604800; // 7 days in seconds

    private String issuer = "lumie-auth-svc";
}
