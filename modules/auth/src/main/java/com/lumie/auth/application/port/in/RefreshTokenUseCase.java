package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.RefreshTokenRequest;
import com.lumie.auth.application.dto.response.TokenResponse;

public interface RefreshTokenUseCase {
    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request the refresh token request
     * @return new token pair
     */
    TokenResponse refresh(RefreshTokenRequest request);
}
