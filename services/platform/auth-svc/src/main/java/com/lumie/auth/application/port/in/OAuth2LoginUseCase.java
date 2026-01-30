package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.response.LoginResponse;

public interface OAuth2LoginUseCase {
    /**
     * Generates OAuth2 authorization URL with state parameter.
     *
     * @param provider the OAuth2 provider (google, kakao)
     * @param tenantSlug the tenant slug to encode in state
     * @return the authorization URL
     */
    String getAuthorizationUrl(String provider, String tenantSlug);

    /**
     * Handles OAuth2 callback and returns tokens.
     *
     * @param provider the OAuth2 provider
     * @param code the authorization code
     * @param state the state parameter containing tenant info
     * @return login response with tokens and user info
     */
    LoginResponse handleCallback(String provider, String code, String state);
}
