package com.lumie.auth.domain.repository;

import com.lumie.auth.domain.entity.AuthToken;

import java.util.Optional;

/**
 * Domain repository interface for token persistence (Redis).
 */
public interface TokenRepository {

    /**
     * Saves a refresh token.
     *
     * @param token the token to save
     */
    void saveRefreshToken(AuthToken token);

    /**
     * Finds a refresh token by user ID and JTI.
     *
     * @param userId the user ID
     * @param jti the JWT ID
     * @return the token if found
     */
    Optional<AuthToken> findRefreshToken(Long userId, String jti);

    /**
     * Deletes a refresh token.
     *
     * @param userId the user ID
     * @param jti the JWT ID
     */
    void deleteRefreshToken(Long userId, String jti);

    /**
     * Deletes all refresh tokens for a user.
     *
     * @param userId the user ID
     */
    void deleteAllRefreshTokensForUser(Long userId);

    /**
     * Adds a token to the blacklist.
     *
     * @param jti the JWT ID
     * @param ttlSeconds the time-to-live in seconds
     */
    void blacklistToken(String jti, long ttlSeconds);

    /**
     * Checks if a token is blacklisted.
     *
     * @param jti the JWT ID
     * @return true if blacklisted
     */
    boolean isBlacklisted(String jti);

    /**
     * Saves session information.
     *
     * @param tenantSlug the tenant slug
     * @param jti the JWT ID
     * @param sessionData the session data as JSON string
     * @param ttlSeconds the time-to-live in seconds
     */
    void saveSession(String tenantSlug, String jti, String sessionData, long ttlSeconds);

    /**
     * Finds session information.
     *
     * @param tenantSlug the tenant slug
     * @param jti the JWT ID
     * @return the session data if found
     */
    Optional<String> findSession(String tenantSlug, String jti);

    /**
     * Deletes session information.
     *
     * @param tenantSlug the tenant slug
     * @param jti the JWT ID
     */
    void deleteSession(String tenantSlug, String jti);
}
