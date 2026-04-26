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
 * User entity stored in public.users table.
 * All users across all tenants are stored here with tenant_id for identification.
 */
@Entity
@Table(name = "users", schema = "public")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String status;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "avatar_seed", length = 50)
    private String avatarSeed;

    private User(String userLoginId, String passwordHash, String name, String phone,
                 Role role, String status, Long tenantId) {
        this.userLoginId = userLoginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.tenantId = tenantId;
    }

    public static User create(String userLoginId, String name, String phone,
                              String passwordHash, Role role, Long tenantId) {
        return new User(userLoginId, passwordHash, name, phone, role, STATUS_ACTIVE, tenantId);
    }

    public static User createOAuth2User(String userLoginId, String name, Long tenantId) {
        return new User(userLoginId, "", name, null, Role.STUDENT, STATUS_ACTIVE, tenantId);
    }

    public boolean isEnabled() {
        return STATUS_ACTIVE.equals(status);
    }

    public void resetPassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void changeLoginId(String newLoginId) {
        this.userLoginId = newLoginId;
    }

    public void setAvatarSeed(String avatarSeed) {
        this.avatarSeed = avatarSeed;
    }
}
