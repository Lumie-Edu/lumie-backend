package com.lumie.auth.application.port.out;

import java.util.Optional;

/**
 * Output port for the OWNER directory living in {@code public.users}.
 *
 * <p>After the user-storage split, {@code public.users} holds only OWNER rows
 * and serves a single purpose: routing root-URL logins (e.g. {@code lumie-edu.com})
 * to the correct tenant schema. All non-OWNER identity (and the OWNER's own
 * password / profile) lives in {@code tenant_*.users}, accessed via {@link UserLookupPort}.
 *
 * <p>Implementations of this port MUST query {@code public.users} explicitly
 * (e.g. via {@link org.springframework.jdbc.core.JdbcTemplate} or an entity bound
 * to {@code schema = "public"}), because Hibernate's multi-tenancy resolver would
 * otherwise route to whatever tenant context is currently set.
 */
public interface UserDirectoryPort {

    /**
     * Look up an OWNER directory entry by login ID.
     *
     * @return the entry if a row exists in {@code public.users} with this
     *         {@code user_login_id} and {@code role = 'OWNER'}, otherwise empty
     */
    Optional<DirectoryEntry> findByUserLoginId(String userLoginId);

    /**
     * Look up an OWNER directory entry by user ID. Used by JWT refresh to
     * resolve {@code tenant_id} when a token is issued before the migration
     * but the user has since moved to the tenant schema.
     */
    Optional<DirectoryEntry> findById(Long userId);

    /**
     * Insert a new OWNER directory entry. Called only during owner registration,
     * paired with an insert into {@code tenant_*.users}; the two writes must be
     * coordinated by the caller (see {@code AuthCommandService.registerOwner}).
     *
     * @param userId       the user's id, MUST equal the id of the matching row
     *                     in {@code tenant_*.users(id)}
     * @param userLoginId  globally unique login id for the OWNER
     * @param tenantId     foreign key into {@code public.tenants(id)}
     * @param email        recovery email; nullable
     */
    void createDirectoryEntry(Long userId, String userLoginId, Long tenantId, String email);

    /**
     * Update the recovery email for an existing directory entry. Mirrors the
     * email column on {@code tenant.users} so future password-recovery flows
     * can find the OWNER by email at the {@code lumie-edu.com} root entry point.
     */
    void updateEmail(Long userId, String email);

    /**
     * Update the login id of an existing directory entry. Paired with the same
     * change against {@code tenant.users.user_login_id}.
     */
    void updateLoginId(Long userId, String newLoginId);

    /**
     * Remove a directory entry. Called when an OWNER is deleted; the matching
     * {@code tenant.users} row is removed via the regular user persistence path.
     */
    void deleteDirectoryEntry(Long userId);

    /**
     * Slim row from {@code public.users} sufficient to route a login to the
     * right tenant. Does NOT include {@code password_hash}, {@code name}, etc.
     * — those live in {@code tenant.users}.
     */
    record DirectoryEntry(
            Long id,
            String userLoginId,
            Long tenantId,
            String email
    ) {
    }
}
