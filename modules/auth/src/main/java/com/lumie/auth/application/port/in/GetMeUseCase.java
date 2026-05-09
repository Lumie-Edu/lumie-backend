package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.response.UserResponse;

public interface GetMeUseCase {
    /**
     * Returns the authenticated user's profile.
     *
     * @param userId the authenticated user's id
     * @param tenantSlug the tenant slug from the request context
     * @return the user's profile data
     */
    UserResponse getMe(Long userId, String tenantSlug);
}
