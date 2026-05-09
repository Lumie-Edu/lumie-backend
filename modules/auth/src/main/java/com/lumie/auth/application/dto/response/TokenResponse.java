package com.lumie.auth.application.dto.response;

import com.lumie.auth.domain.vo.TokenPair;
import lombok.Builder;

@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
    public static TokenResponse from(TokenPair tokenPair) {
        return TokenResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .accessExpiresIn(tokenPair.accessExpiresIn())
                .refreshExpiresIn(tokenPair.refreshExpiresIn())
                .build();
    }
}
