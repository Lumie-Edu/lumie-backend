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
 * User entity stored in tenant-specific schemas.
 * Each tenant has their own users table in their schema (tenant_{slug}.users).
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

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String status;

    @Column(name = "oauth_provider")
    private String oauthProvider;

    private User(String email, String passwordHash, String name, Role role, String status, String oauthProvider) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.status = status;
        this.oauthProvider = oauthProvider;
    }

    public static User create(String email, String name, String passwordHash, Role role) {
        return new User(email, passwordHash, name, role, STATUS_ACTIVE, null);
    }

    public static User createOAuth2User(String email, String name, String provider) {
        return new User(email, "", name, Role.STUDENT, STATUS_ACTIVE, provider);
    }

    public boolean isEnabled() {
        return STATUS_ACTIVE.equals(status);
    }
}
