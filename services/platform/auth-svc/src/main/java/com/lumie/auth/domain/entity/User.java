package com.lumie.auth.domain.entity;

import com.lumie.auth.domain.vo.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User entity stored in the per-tenant {@code users} table.
 *
 * <p>Hibernate multi-tenancy resolves the actual schema at session acquisition
 * via {@link com.lumie.common.tenant.TenantConnectionProvider}. Callers MUST
 * have {@code TenantContextHolder} set to the desired tenant before invoking
 * any JPA operation against this entity.
 *
 * <p>The legacy {@code public.users} table (which once held all users globally)
 * has been slimmed down to an OWNER-only directory; access it via
 * {@link com.lumie.auth.application.port.out.UserDirectoryPort}, NOT via this
 * entity.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    private String status;

    @Column(name = "avatar_seed", length = 50)
    private String avatarSeed;

    private User(String userLoginId, String passwordHash, String name, String phone, Role role, String status) {
        this.userLoginId = userLoginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.avatarSeed = com.lumie.auth.domain.vo.AvatarSeeds.randomSeed();
    }

    public static User create(String userLoginId, String name, String phone,
                              String passwordHash, Role role) {
        return new User(userLoginId, passwordHash, name, phone, role, STATUS_ACTIVE);
    }

    public static User createOAuth2User(String userLoginId, String name, Role role) {
        return new User(userLoginId, "", name, null, role, STATUS_ACTIVE);
    }

    public boolean isEnabled() {
        return STATUS_ACTIVE.equals(status);
    }

    public void resetPassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void updateProfile(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public void changeLoginId(String newLoginId) {
        this.userLoginId = newLoginId;
    }

    public void setAvatarSeed(String avatarSeed) {
        this.avatarSeed = avatarSeed;
    }
}
