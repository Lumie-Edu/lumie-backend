package com.lumie.auth.domain.vo;

/**
 * Immutable value object representing a pair of access and refresh tokens.
 *
 * @param accessToken the JWT access token
 * @param refreshToken the refresh token for obtaining new access tokens
 * @param accessExpiresIn seconds until access token expiration
 * @param refreshExpiresIn seconds until refresh token expiration
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
    public static TokenPair of(String accessToken, String refreshToken,
                               long accessExpiresIn, long refreshExpiresIn) {
        return new TokenPair(accessToken, refreshToken, accessExpiresIn, refreshExpiresIn);
    }
}
