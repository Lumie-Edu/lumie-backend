package com.lumie.admin.domain.entity;

import com.lumie.admin.domain.vo.EmploymentStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

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

    @Formula("(SELECT u.role FROM public.users u WHERE u.id = user_id)")
    private String role;

    @Column(name = "user_login_id", nullable = false, length = 50)
    private String userLoginId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "admin_academies", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "academy_id")
    private Set<Long> academyIds = new HashSet<>();

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Builder
    private Admin(Long userId, String userLoginId, String name, String phone, String email,
                 Set<Long> academyIds, String adminMemo,
                 EmploymentStatus employmentStatus, Boolean isActive) {
        this.userId = userId;
        this.userLoginId = userLoginId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.academyIds = academyIds != null ? academyIds : new HashSet<>();
        this.adminMemo = adminMemo;
        this.employmentStatus = employmentStatus != null ? employmentStatus : EmploymentStatus.ACTIVE;
        this.isActive = isActive != null ? isActive : true;
    }

    public static Admin create(Long userId, String userLoginId, String name, String phone, String email,
                               Set<Long> academyIds, String adminMemo) {
        return Admin.builder()
                .userId(userId)
                .userLoginId(userLoginId)
                .name(name)
                .phone(phone)
                .email(email)
                .academyIds(academyIds)
                .adminMemo(adminMemo)
                .employmentStatus(EmploymentStatus.ACTIVE)
                .isActive(true)
                .build();
    }

    public void updateLoginId(String newLoginId) {
        this.userLoginId = newLoginId;
    }

    public void updateInfo(String name, String phone, String email, String adminMemo) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (email != null) {
            this.email = email;
        }
        if (adminMemo != null) {
            this.adminMemo = adminMemo;
        }
    }

    public void setAcademyIds(Set<Long> academyIds) {
        this.academyIds.clear();
        if (academyIds != null) {
            this.academyIds.addAll(academyIds);
        }
    }

    public String getAdminName() {
        return name;
    }

    public String getAdminPhone() {
        return phone;
    }

    public String getAdminEmail() {
        return email != null ? email : userLoginId;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }

    public void deactivate() {
        this.isActive = false;
        this.employmentStatus = EmploymentStatus.INACTIVE;
    }

    public void activate() {
        this.isActive = true;
        this.employmentStatus = EmploymentStatus.ACTIVE;
    }

    public void terminate() {
        this.isActive = false;
        this.employmentStatus = EmploymentStatus.TERMINATED;
    }
}
