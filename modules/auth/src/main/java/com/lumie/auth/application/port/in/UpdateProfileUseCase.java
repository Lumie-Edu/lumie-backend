package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.UpdateProfileRequest;
import com.lumie.auth.application.dto.response.UserResponse;

public interface UpdateProfileUseCase {
    /**
     * Updates the authenticated user's profile (name, phone) and returns the
     * refreshed UserResponse.
     */
    UserResponse updateProfile(Long userId, String tenantSlug, UpdateProfileRequest request);
}
