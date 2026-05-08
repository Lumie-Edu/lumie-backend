package com.lumie.auth.application.port.out;

import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.common.auth.Role;

import java.util.Optional;

/**
 * Tenant-scoped user lookup port.
 *
 * <p>All operations require {@code TenantContextHolder} to be set to the
 * target tenant before invocation. The persistence adapter routes queries to
 * {@code tenant_*.users} via Hibernate multi-tenancy.
 *
 * <p>For OWNER directory routing (the {@code lumie-edu.com} root entry point),
 * use {@link UserDirectoryPort} which queries {@code public.users} explicitly.
 */
public interface UserLookupPort {

    Optional<UserData> findByUserLoginId(String userLoginId);

    Optional<UserData> findById(Long userId);

    /**
     * Find an OAuth2 user by login ID, or create a new row in the current
     * tenant context if none exists. Defaults to {@code STUDENT} role for
     * backward compatibility (only students self-register via OAuth today).
     */
    UserData findOrCreateOAuth2User(String userLoginId, String name, String provider);

    /**
     * Create a new user in the current tenant's {@code users} table. The
     * tenant is resolved implicitly from {@code TenantContextHolder}.
     */
    UserData createUser(String userLoginId, String name, String phone, String passwordHash, Role role);

    boolean existsByUserLoginId(String userLoginId);

    boolean deleteUser(Long userId);

    boolean resetPassword(Long userId, String newPasswordHash);

    ChangeLoginIdResult changeLoginId(Long userId, String newLoginId);

    record ChangeLoginIdResult(boolean success, String message, String newLoginId) {}

    boolean updateAvatarSeed(Long userId, String avatarSeed);

    boolean updateProfile(Long userId, String name, String phone, String email);

    record UserData(
            Long id,
            String userLoginId,
            String name,
            String phone,
            String email,
            String passwordHash,
            Role role,
            Long tenantId,
            boolean enabled,
            String avatarSeed
    ) {
        public UserResponse toUserResponse(String tenantSlug) {
            return UserResponse.builder()
                    .id(id)
                    .userLoginId(userLoginId)
                    .name(name)
                    .phone(phone)
                    .email(email)
                    .role(role)
                    .tenantSlug(tenantSlug)
                    .tenantId(tenantId)
                    .avatarSeed(avatarSeed)
                    .build();
        }
    }
}
