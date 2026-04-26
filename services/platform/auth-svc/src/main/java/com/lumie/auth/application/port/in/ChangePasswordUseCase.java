package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.ChangePasswordRequest;

public interface ChangePasswordUseCase {
    /**
     * Verifies the current password and replaces it with a new one. All other refresh
     * sessions for the user are revoked.
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
