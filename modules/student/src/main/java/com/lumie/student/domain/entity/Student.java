package com.lumie.student.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "user_login_id", nullable = false, length = 50)
    private String userLoginId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    // The old columns remain dual-written for rolling compatibility; new reads prefer User.
    @Formula("(SELECT u.name FROM users u WHERE u.id = user_id)")
    private String canonicalName;

    @Formula("(SELECT u.phone FROM users u WHERE u.id = user_id)")
    private String canonicalPhone;

    @Column(name = "student_highschool", length = 100)
    private String studentHighschool;

    @Column(name = "student_birth_year")
    private Integer studentBirthYear;

    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Column(name = "student_memo", columnDefinition = "TEXT")
    private String studentMemo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    private Student(Long userId, String userLoginId, String name, String phone,
                    String studentHighschool,
                    Integer studentBirthYear, String parentPhone,
                    String studentMemo, Boolean isActive) {
        this.userId = userId;
        this.userLoginId = userLoginId;
        this.name = name;
        this.phone = com.lumie.common.util.PhoneNumberUtils.toDigits(phone);
        this.studentHighschool = studentHighschool;
        this.studentBirthYear = studentBirthYear;
        this.parentPhone = normalizeBlankParentPhone(parentPhone);
        this.studentMemo = studentMemo;
        this.isActive = isActive != null ? isActive : true;
    }

    public static Student create(Long userId, String userLoginId, String name, String phone,
                                 String studentHighschool,
                                 Integer studentBirthYear, String parentPhone,
                                 String studentMemo) {
        return Student.builder()
                .userId(userId)
                .userLoginId(userLoginId)
                .name(name)
                .phone(phone)
                .studentHighschool(studentHighschool)
                .studentBirthYear(studentBirthYear)
                .parentPhone(parentPhone)
                .studentMemo(studentMemo)
                .isActive(true)
                .build();
    }

    public void synchronizeIdentity(String name, String phone, boolean nameChanged, boolean phoneChanged) {
        if (nameChanged) {
            this.name = name;
            this.canonicalName = name;
        }
        if (phoneChanged) {
            this.phone = com.lumie.common.util.PhoneNumberUtils.toDigits(phone);
            this.canonicalPhone = this.phone;
        }
    }

    public void updateInfo(String name, String phone, String studentHighschool,
                           Integer studentBirthYear, String parentPhone,
                           String studentMemo) {
        if (name != null && !name.isBlank()) {
            this.name = name;
            this.canonicalName = name;
        }
        if (phone != null) {
            this.phone = phone.isBlank() ? null : com.lumie.common.util.PhoneNumberUtils.toDigits(phone);
            this.canonicalPhone = this.phone;
        }
        if (studentHighschool != null) {
            this.studentHighschool = studentHighschool;
        }
        if (studentBirthYear != null) {
            this.studentBirthYear = studentBirthYear;
        }
        if (parentPhone != null) {
            this.parentPhone = normalizeBlankParentPhone(parentPhone);
        }
        if (studentMemo != null) {
            this.studentMemo = studentMemo;
        }
    }

    public void updateLoginId(String newLoginId) {
        this.userLoginId = newLoginId;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }

    public String getStudentName() {
        return canonicalName != null ? canonicalName : name;
    }

    public String getStudentPhone() {
        return canonicalName != null ? canonicalPhone : phone;
    }

    public String getName() {
        return getStudentName();
    }

    public String getPhone() {
        return getStudentPhone();
    }

    public String getStudentEmail() {
        return userLoginId;
    }

    private static String normalizeBlankParentPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return com.lumie.common.util.PhoneNumberUtils.toDigits(phone);
    }
}
