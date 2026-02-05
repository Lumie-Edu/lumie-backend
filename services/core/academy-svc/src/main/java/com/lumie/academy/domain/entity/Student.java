package com.lumie.academy.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Column(name = "student_highschool", length = 100)
    private String studentHighschool;

    @Column(name = "student_birth_year")
    private Integer studentBirthYear;

    @Column(name = "student_memo", columnDefinition = "TEXT")
    private String studentMemo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    private Student(Long userId, String userLoginId, String name, String phone,
                   Academy academy, String studentHighschool,
                   Integer studentBirthYear, String studentMemo, Boolean isActive) {
        this.userId = userId;
        this.userLoginId = userLoginId;
        this.name = name;
        this.phone = phone;
        this.academy = academy;
        this.studentHighschool = studentHighschool;
        this.studentBirthYear = studentBirthYear;
        this.studentMemo = studentMemo;
        this.isActive = isActive != null ? isActive : true;
    }

    public static Student create(Long userId, String userLoginId, String name, String phone,
                                 Academy academy, String studentHighschool,
                                 Integer studentBirthYear, String studentMemo) {
        return Student.builder()
                .userId(userId)
                .userLoginId(userLoginId)
                .name(name)
                .phone(phone)
                .academy(academy)
                .studentHighschool(studentHighschool)
                .studentBirthYear(studentBirthYear)
                .studentMemo(studentMemo)
                .isActive(true)
                .build();
    }

    public void updateInfo(String name, String phone, String studentHighschool,
                          Integer studentBirthYear, String studentMemo) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (studentHighschool != null) {
            this.studentHighschool = studentHighschool;
        }
        if (studentBirthYear != null) {
            this.studentBirthYear = studentBirthYear;
        }
        if (studentMemo != null) {
            this.studentMemo = studentMemo;
        }
    }

    public void changeAcademy(Academy academy) {
        if (academy != null) {
            this.academy = academy;
        }
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
        return name;
    }

    public String getStudentPhone() {
        return phone;
    }

    public String getStudentEmail() {
        return userLoginId;
    }
}
