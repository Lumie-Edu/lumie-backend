package com.lumie.auth.domain.entity;

import com.lumie.common.auth.Role;
import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.auth.domain.vo.UserStatus;
import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User entity stored in the tenant-scoped {@code users} table.
 *
 * <p>Postgres RLS enforces tenant isolation: every query against this entity
 * is automatically filtered by {@code app.tenant_id} (set per-tx by
 * {@link com.lumie.common.tenant.RlsTenantContextAspect}). Callers MUST
 * have {@code TenantContextHolder.setTenantId(...)} populated before
 * invoking any JPA operation, or rows will be invisible.
 *
 * <p>The root-entry OWNER directory lives in {@code public.owner_directory};
 * access it via {@link com.lumie.auth.application.port.out.UserDirectoryPort},
 * NOT via this entity.
 *
 * <p>Audit timestamps ({@code created_at}, {@code updated_at}) and
 * {@code tenant_id} binding are provided by {@link TenantScopedEntity}.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "user_login_id", nullable = false, unique = true)
    private String userLoginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column
    private String phone;

    @Column
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "avatar_seed", length = 50)
    private String avatarSeed;

    private User(String userLoginId, String passwordHash, String name, String phone, Role role, UserStatus status) {
        this.userLoginId = userLoginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = PhoneNumberUtils.toDigits(phone);
        this.role = role;
        this.status = status;
        this.avatarSeed = com.lumie.auth.domain.vo.AvatarSeeds.randomSeed();
    }

    public static User create(String userLoginId, String name, String phone,
                              String passwordHash, Role role) {
        return new User(userLoginId, passwordHash, name, phone, role, UserStatus.ACTIVE);
    }

    public boolean isEnabled() {
        return status != null && status.isEnabled();
    }

    public void resetPassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void updateProfile(String name, String phone, String email) {
        this.name = name;
        this.phone = phone == null || phone.isBlank() ? null : PhoneNumberUtils.toDigits(phone);
        this.email = email == null || email.isBlank() ? null : email;
    }

    public void updateIdentity(String name, String phone, String email) {
        if (name != null && !name.isBlank()) this.name = name;
        if (phone != null) this.phone = phone.isBlank() ? null : PhoneNumberUtils.toDigits(phone);
        if (email != null) this.email = email;
    }

    public void changeLoginId(String newLoginId) {
        this.userLoginId = newLoginId;
    }

    public void setAvatarSeed(String avatarSeed) {
        this.avatarSeed = avatarSeed;
    }
}
