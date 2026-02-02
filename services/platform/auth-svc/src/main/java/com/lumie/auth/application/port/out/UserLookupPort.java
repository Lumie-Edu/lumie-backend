package com.lumie.auth.application.port.out;

import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.domain.vo.Role;

import java.util.Optional;

/**
 * Output port for user lookup operations from public.users table.
 * All users are stored in a single global table with tenant_id for identification.
 */
public interface UserLookupPort {

    /**
     * Finds a user by login ID.
     *
     * @param userLoginId the user's login ID
     * @return optional containing user data if found
     */
    Optional<UserData> findByUserLoginId(String userLoginId);

    /**
     * Finds a user by ID.
     *
     * @param userId the user's ID
     * @return optional containing user data if found
     */
    Optional<UserData> findById(Long userId);

    /**
     * Finds or creates a user for OAuth2 login.
     *
     * @param userLoginId the user's login ID (derived from OAuth provider)
     * @param name the user's name
     * @param tenantId the tenant ID
     * @param provider the OAuth2 provider (google, kakao)
     * @return the user data
     */
    UserData findOrCreateOAuth2User(String userLoginId, String name, Long tenantId, String provider);

    /**
     * Creates a new user with login ID/password credentials.
     *
     * @param userLoginId the user's login ID
     * @param name the user's name
     * @param phone the user's phone number
     * @param passwordHash the hashed password
     * @param role the user's role
     * @param tenantId the tenant ID
     * @return the created user data
     */
    UserData createUser(String userLoginId, String name, String phone, String passwordHash, Role role, Long tenantId);

    /**
     * Checks if a user with the given login ID exists.
     *
     * @param userLoginId the user's login ID
     * @return true if the user exists
     */
    boolean existsByUserLoginId(String userLoginId);

    /**
     * User data record from public.users table.
     */
    record UserData(
            Long id,
            String userLoginId,
            String name,
            String phone,
            String passwordHash,
            Role role,
            Long tenantId,
            boolean enabled
    ) {
        public UserResponse toUserResponse(String tenantSlug) {
            return UserResponse.builder()
                    .id(id)
                    .userLoginId(userLoginId)
                    .name(name)
                    .role(role)
                    .tenantSlug(tenantSlug)
                    .tenantId(tenantId)
                    .build();
        }
    }
}
