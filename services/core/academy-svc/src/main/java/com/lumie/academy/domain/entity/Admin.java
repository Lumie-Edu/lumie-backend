package com.lumie.academy.domain.entity;

import com.lumie.academy.domain.vo.Role;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id")
    private Academy academy;

    @Column(name = "admin_type", nullable = false, length = 20)
    private String adminType;

    @Column(name = "permissions", columnDefinition = "jsonb")
    private String permissions;

    @Builder
    private Admin(User user, Academy academy, String adminType, String permissions) {
        this.user = user;
        this.academy = academy;
        this.adminType = adminType;
        this.permissions = permissions;
    }

    public static Admin create(String email, String passwordHash, String name, String phone,
                               Academy academy, String adminType) {
        User user = new User(email, passwordHash, name, phone, Role.ADMIN) {};
        return Admin.builder()
                .user(user)
                .academy(academy)
                .adminType(adminType)
                .build();
    }

    public void updateInfo(String name, String phone, String adminType) {
        if (adminType != null && !adminType.isBlank()) {
            this.adminType = adminType;
        }
    }

    public void updatePermissions(String permissions) {
        this.permissions = permissions;
    }

    public void assignToAcademy(Academy academy) {
        this.academy = academy;
    }

    public String getAdminName() {
        return user.getName();
    }

    public String getAdminPhone() {
        return user.getPhone();
    }

    public String getAdminEmail() {
        return user.getEmail();
    }

    public Long getUserId() {
        return user.getId();
    }

    public boolean isActive() {
        return user.isActive();
    }

    public void deactivate() {
        this.user.deactivate();
    }

    public void activate() {
        this.user.activate();
    }
}
