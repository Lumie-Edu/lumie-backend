package com.lumie.auth.application.port.out;

import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.domain.vo.Role;

import java.util.Optional;

/**
 * Output port for user lookup operations from tenant schema.
 */
public interface UserLookupPort {

    /**
     * Finds a user by email in the tenant's schema.
     *
     * @param schemaName the tenant's PostgreSQL schema name
     * @param email the user's email
     * @return optional containing user data if found
     */
    Optional<UserData> findByEmail(String schemaName, String email);

    /**
     * Finds a user by ID in the tenant's schema.
     *
     * @param schemaName the tenant's PostgreSQL schema name
     * @param userId the user's ID
     * @return optional containing user data if found
     */
    Optional<UserData> findById(String schemaName, Long userId);

    /**
     * Finds or creates a user for OAuth2 login.
     *
     * @param schemaName the tenant's PostgreSQL schema name
     * @param email the user's email
     * @param name the user's name
     * @param provider the OAuth2 provider (google, kakao)
     * @return the user data
     */
    UserData findOrCreateOAuth2User(String schemaName, String email, String name, String provider);

    /**
     * User data record from tenant schema.
     */
    record UserData(
            Long id,
            String email,
            String name,
            String passwordHash,
            Role role,
            boolean enabled
    ) {
        public UserResponse toUserResponse(String tenantSlug, Long tenantId) {
            return UserResponse.builder()
                    .id(id)
                    .email(email)
                    .name(name)
                    .role(role)
                    .tenantSlug(tenantSlug)
                    .tenantId(tenantId)
                    .build();
        }
    }
}
