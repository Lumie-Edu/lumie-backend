package com.lumie.auth.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface AuthService {

    CreateUserResult createUser(String name, String phone, String role, Long tenantId);

    DeleteUserResult deleteUser(Long userId);

    ResetPasswordResult resetPassword(Long userId);

    ChangeLoginIdResult changeLoginId(Long userId, String newLoginId);

    boolean updateIdentity(Long userId, String name, String phone, String email);

    /**
     * Batch lookup of avatar seeds keyed by user ID. Users without an
     * avatar seed are simply omitted from the returned map.
     */
    Map<Long, String> getAvatarSeeds(Collection<Long> userIds);

    /** Existing tenant users only; absent IDs are omitted from the result. */
    Map<Long, String> getUserRoles(Collection<Long> userIds);

    default Optional<TenantOwnerData> getTenantOwner() {
        return Optional.empty();
    }

    record TenantOwnerData(Long userId, String userLoginId, String name, String phone,
                           String email, String avatarSeed, java.time.Instant createdAt,
                           java.time.Instant updatedAt) {}

    record CreateUserResult(
            boolean success,
            String message,
            Long userId,
            String userLoginId
    ) {}

    record DeleteUserResult(
            boolean success,
            String message
    ) {}

    record ResetPasswordResult(
            boolean success,
            String message
    ) {}

    record ChangeLoginIdResult(
            boolean success,
            String message,
            String newLoginId
    ) {}
}
