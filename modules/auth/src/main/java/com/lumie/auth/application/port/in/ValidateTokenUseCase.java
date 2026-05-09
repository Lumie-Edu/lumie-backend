package com.lumie.auth.application.port.in;

import com.lumie.auth.domain.vo.TokenClaims;

import java.util.Optional;

public interface ValidateTokenUseCase {
    /**
     * Validates a token and returns the claims if valid.
     *
     * @param token the token to validate
     * @return optional containing token claims if valid, empty if invalid
     */
    Optional<TokenClaims> validate(String token);

    /**
     * Checks if a token is valid without returning claims.
     *
     * @param token the token to check
     * @return true if the token is valid
     */
    boolean isValid(String token);
}
