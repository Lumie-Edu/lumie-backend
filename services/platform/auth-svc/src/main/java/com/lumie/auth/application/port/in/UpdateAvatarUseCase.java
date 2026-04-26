package com.lumie.auth.application.port.in;

public interface UpdateAvatarUseCase {
    /**
     * Updates the current user's avatar seed.
     *
     * @param userId the authenticated user's id
     * @param avatarSeed one of the curated picker values
     */
    void updateAvatar(Long userId, String avatarSeed);
}
