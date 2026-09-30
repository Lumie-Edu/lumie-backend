package com.lumie.auth.application.port.out;

import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.common.auth.Role;

import java.time.Instant;
import java.util.Optional;
import java.util.Collection;
import java.util.Map;

/**
 * RLS-scoped user lookup port.
 *
 * <p>All operations require {@code TenantContextHolder} to be bound to the
 * target tenant before invocation. {@code tenantId} is the load-bearing value:
 * Postgres RLS filters visibility via {@code app.tenant_id}, while the slug is
 * retained for routing, logging, and headers.
 *
 * <p>This port models tenant-scoped user rows in Lumie's single-schema RLS
 * design. It is not the deleted Hibernate schema multi-tenancy flow and does
 * not route to {@code tenant_*.users}.
 *
 * <p>For OWNER directory routing at the {@code lumie-edu.com} root entry point,
 * use {@link UserDirectoryPort}, which reads the platform-scoped
 * {@code public.owner_directory} before any tenant RLS context exists.
 */
public interface UserLookupPort {

    Optional<UserData> findByUserLoginId(String userLoginId);

    Optional<UserData> findById(Long userId);

    Map<Long, String> getAvatarSeeds(Collection<Long> userIds);

    /**
     * Create a new user in the current tenant's RLS-scoped {@code users}
     * rows. The caller must bind the target {@code tenantId} in
     * {@code TenantContextHolder}; that value governs row visibility and
     * downstream provisioning.
     */
    UserData createUser(String userLoginId, String name, String phone, String passwordHash, Role role);

    boolean existsByUserLoginId(String userLoginId);

    boolean deleteUser(Long userId);

    boolean resetPassword(Long userId, String newPasswordHash);

    ChangeLoginIdResult changeLoginId(Long userId, String newLoginId);

    record ChangeLoginIdResult(boolean success, String message, String newLoginId) {}

    boolean updateAvatarSeed(Long userId, String avatarSeed);

    boolean updateProfile(Long userId, String name, String phone, String email);

    boolean updateIdentity(Long userId, String name, String phone, String email);

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
            return toUserResponse(tenantSlug, null);
        }

        public UserResponse toUserResponse(String tenantSlug, Instant tenantOnboardingCompletedAt) {
            return UserResponse.builder()
                    .id(id)
                    .userLoginId(userLoginId)
                    .name(name)
                    .phone(phone)
                    .email(email)
                    .role(role)
                    .tenantSlug(tenantSlug)
                    .tenantId(tenantId)
                    .tenantOnboardingCompletedAt(tenantOnboardingCompletedAt)
                    .avatarSeed(avatarSeed)
                    .build();
        }
    }
}
