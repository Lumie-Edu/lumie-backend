package com.lumie.academy.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "admins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "user_login_id", nullable = false, length = 50)
    private String userLoginId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "admin_academies",
        joinColumns = @JoinColumn(name = "admin_id"),
        inverseJoinColumns = @JoinColumn(name = "academy_id")
    )
    private Set<Academy> academies = new HashSet<>();

    @Column(name = "admin_position", length = 50)
    private String adminPosition = "조교";

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Builder
    private Admin(Long userId, String userLoginId, String name, String phone,
                 Set<Academy> academies, String adminPosition, String adminMemo, Boolean isActive) {
        this.userId = userId;
        this.userLoginId = userLoginId;
        this.name = name;
        this.phone = phone;
        this.academies = academies != null ? academies : new HashSet<>();
        this.adminPosition = adminPosition != null ? adminPosition : "조교";
        this.adminMemo = adminMemo;
        this.isActive = isActive != null ? isActive : true;
    }

    public static Admin create(Long userId, String userLoginId, String name, String phone,
                               Set<Academy> academies, String adminPosition, String adminMemo) {
        return Admin.builder()
                .userId(userId)
                .userLoginId(userLoginId)
                .name(name)
                .phone(phone)
                .academies(academies)
                .adminPosition(adminPosition)
                .adminMemo(adminMemo)
                .isActive(true)
                .build();
    }

    public void updateInfo(String name, String phone, String adminPosition, String adminMemo) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (adminPosition != null) {
            this.adminPosition = adminPosition;
        }
        if (adminMemo != null) {
            this.adminMemo = adminMemo;
        }
    }

    public void addAcademy(Academy academy) {
        this.academies.add(academy);
    }

    public void removeAcademy(Academy academy) {
        this.academies.remove(academy);
    }

    public void setAcademies(Set<Academy> academies) {
        this.academies.clear();
        if (academies != null) {
            this.academies.addAll(academies);
        }
    }

    public String getAdminName() {
        return name;
    }

    public String getAdminPhone() {
        return phone;
    }

    public String getAdminEmail() {
        return userLoginId;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
