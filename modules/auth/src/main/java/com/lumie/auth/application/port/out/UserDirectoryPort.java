package com.lumie.auth.application.port.out;

import java.util.Optional;

/**
 * Output port for the OWNER directory living in {@code public.owner_directory}.
 *
 * <p>In the current RLS model, this platform-scoped table exists only to map
 * root-domain logins (for example {@code lumie-edu.com}) to a tenant before
 * any tenant context is available. The returned {@code tenantId} is
 * load-bearing because it is what later binds {@code app.tenant_id} for RLS
 * visibility and provisioning.
 *
 * <p>Tenant-scoped user state lives in the regular RLS-guarded {@code users}
 * rows accessed via {@link UserLookupPort}. This is not the deleted
 * schema-per-tenant model and does not involve {@code tenant_*.users}.
 *
 * <p>Implementations of this port must read {@code public.owner_directory}
 * directly, because root login happens before any tenant RLS context exists.
 */
public interface UserDirectoryPort {

    /**
     * Look up an OWNER directory entry by login ID.
     *
     * @return the entry if a row exists in {@code public.owner_directory} with
     *         this {@code user_login_id}, otherwise empty
     */
    Optional<DirectoryEntry> findByUserLoginId(String userLoginId);

    /**
     * Look up an OWNER directory entry by user ID. Used when auth flows need
     * to recover the authoritative {@code tenantId} so a later
     * {@code TenantContextHolder} binding can re-establish RLS visibility.
     */
    Optional<DirectoryEntry> findById(Long userId);

    /**
     * Insert a new OWNER directory entry. Called only during owner registration,
     * paired with an insert into the matching RLS-scoped {@code users} row; the
     * two writes must be coordinated by the caller (see
     * {@code AuthRegistrationService.registerOwner}).
     *
     * @param userId       the user's id, MUST equal the id of the matching row
     *                     in {@code users(id)}
     * @param userLoginId  globally unique login id for the OWNER
     * @param tenantId     foreign key into {@code public.tenants(id)} and the
     *                     value later used to bind RLS context
     * @param email        recovery email; nullable
     */
    void createDirectoryEntry(Long userId, String userLoginId, Long tenantId, String email);

    /**
     * Update the recovery email for an existing directory entry. Mirrors the
     * email column on the matching RLS-scoped {@code users} row so future
     * password-recovery flows
     * can find the OWNER by email at the {@code lumie-edu.com} root entry point.
     */
    void updateEmail(Long userId, String email);

    /**
     * Update the login id of an existing directory entry. Paired with the same
     * change against the matching RLS-scoped {@code users.user_login_id}.
     */
    void updateLoginId(Long userId, String newLoginId);

    /**
     * Remove a directory entry. Called when an OWNER is deleted; the matching
     * RLS-scoped {@code users} row is removed via the regular user persistence
     * path.
     */
    void deleteDirectoryEntry(Long userId);

    /**
     * Slim row from {@code public.owner_directory} sufficient to route a login
     * to the right tenant and bind RLS context. Does not include
     * {@code password_hash}, {@code name}, etc.; those live in the regular
     * tenant-scoped {@code users} row.
     */
    record DirectoryEntry(
            Long id,
            String userLoginId,
            Long tenantId,
            String email
    ) {
    }
}
