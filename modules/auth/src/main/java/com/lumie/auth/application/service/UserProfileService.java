package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.ChangePasswordRequest;
import com.lumie.auth.application.dto.request.UpdateProfileRequest;
import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Self-service profile + credential operations for an authenticated user.
 * Password change forcefully invalidates all sessions/refresh tokens to
 * close out compromised devices.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserLookupPort userLookupPort;
    private final TokenPersistencePort tokenPersistencePort;
    private final PasswordEncoder passwordEncoder;

    public UserResponse updateProfile(Long userId, String tenantSlug, UpdateProfileRequest request) {
        log.info("Profile update request for user: {}", userId);

        boolean updated = userLookupPort.updateProfile(userId, request.name(), request.phone(), request.email());
        if (!updated) {
            throw new ResourceNotFoundException("User", String.valueOf(userId));
        }

        UserData user = userLookupPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));
        return user.toUserResponse(tenantSlug);
    }

    public void changePassword(Long userId, String tenantSlug, ChangePasswordRequest request) {
        log.info("Password change request for user: {}", userId);

        UserData user = userLookupPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));

        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new IllegalStateException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.passwordHash())) {
            throw new IllegalArgumentException("New password must differ from the current password");
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        userLookupPort.resetPassword(userId, newHash);

        tokenPersistencePort.deleteAllRefreshTokensForUser(userId);
        tokenPersistencePort.deleteAllSessionsForUser(tenantSlug, userId);

        log.info("Password changed and refresh sessions revoked for user: {}", userId);
    }
}
