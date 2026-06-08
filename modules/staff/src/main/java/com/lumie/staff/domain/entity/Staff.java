package com.lumie.staff.domain.entity;

import com.lumie.staff.domain.vo.EmploymentStatus;
import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "staff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Formula("(SELECT u.role FROM users u WHERE u.id = user_id)")
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

    @Column(name = "staff_memo", columnDefinition = "TEXT")
    private String staffMemo;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Builder
    private Staff(Long userId, String userLoginId, String name, String phone, String email,
                 String staffMemo,
                 EmploymentStatus employmentStatus, Boolean isActive) {
        this.userId = userId;
        this.userLoginId = userLoginId;
        this.name = name;
        this.phone = com.lumie.common.util.PhoneNumberUtils.toDigits(phone);
        this.email = email;
        this.staffMemo = staffMemo;
        this.employmentStatus = employmentStatus != null ? employmentStatus : EmploymentStatus.ACTIVE;
        this.isActive = isActive != null ? isActive : true;
    }

    public static Staff create(Long userId, String userLoginId, String name, String phone, String email,
                               String staffMemo) {
        return Staff.builder()
                .userId(userId)
                .userLoginId(userLoginId)
                .name(name)
                .phone(phone)
                .email(email)
                .staffMemo(staffMemo)
                .employmentStatus(EmploymentStatus.ACTIVE)
                .isActive(true)
                .build();
    }

    public void updateLoginId(String newLoginId) {
        this.userLoginId = newLoginId;
    }

    public void updateInfo(String name, String phone, String email, String staffMemo) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = com.lumie.common.util.PhoneNumberUtils.toDigits(phone);
        }
        if (email != null) {
            this.email = email;
        }
        if (staffMemo != null) {
            this.staffMemo = staffMemo;
        }
    }

    public String getStaffName() {
        return name;
    }

    public String getStaffPhone() {
        return phone;
    }

    public String getStaffEmail() {
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
