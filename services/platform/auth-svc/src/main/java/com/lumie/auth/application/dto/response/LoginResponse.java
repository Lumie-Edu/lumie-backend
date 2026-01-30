package com.lumie.auth.application.dto.response;

import com.lumie.auth.domain.vo.TokenPair;
import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn,
        UserResponse user
) {
    public static LoginResponse of(TokenPair tokenPair, UserResponse user) {
        return LoginResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .accessExpiresIn(tokenPair.accessExpiresIn())
                .refreshExpiresIn(tokenPair.refreshExpiresIn())
                .user(user)
                .build();
    }
}
