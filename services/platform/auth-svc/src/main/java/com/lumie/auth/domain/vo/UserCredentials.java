package com.lumie.auth.domain.vo;

/**
 * Immutable value object representing user credentials.
 *
 * @param email the user's email address
 * @param passwordHash the hashed password
 */
public record UserCredentials(
        String email,
        String passwordHash
) {
    public static UserCredentials of(String email, String passwordHash) {
        return new UserCredentials(email, passwordHash);
    }
}
