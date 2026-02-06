package com.lumie.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cookie")
public class CookieConfig {

    private String domain = "";

    private boolean secure = true;

    private boolean httpOnly = true;

    private String sameSite = "Lax";

    private String path = "/";

    private String accessTokenName = "lumie_access_token";

    private String refreshTokenName = "lumie_refresh_token";
}
