package com.lumie.academy.domain.entity;

import com.lumie.academy.domain.vo.Role;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Column(name = "student_number", length = 50)
    private String studentNumber;

    @Column(name = "grade", length = 20)
    private String grade;

    @Column(name = "school_name", length = 100)
    private String schoolName;

    @Column(name = "parent_name", length = 100)
    private String parentName;

    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Builder
    private Student(User user, Academy academy, String studentNumber, String grade,
                   String schoolName, String parentName, String parentPhone, LocalDate enrollmentDate) {
        this.user = user;
        this.academy = academy;
        this.studentNumber = studentNumber;
        this.grade = grade;
        this.schoolName = schoolName;
        this.parentName = parentName;
        this.parentPhone = parentPhone;
        this.enrollmentDate = enrollmentDate;
        this.status = "ACTIVE";
    }

    public static Student create(String email, String passwordHash, String name, String phone,
                                 Academy academy, String studentNumber, String grade,
                                 String schoolName, String parentName, String parentPhone) {
        User user = new User(email, passwordHash, name, phone, Role.STUDENT) {};
        return Student.builder()
                .user(user)
                .academy(academy)
                .studentNumber(studentNumber)
                .grade(grade)
                .schoolName(schoolName)
                .parentName(parentName)
                .parentPhone(parentPhone)
                .enrollmentDate(LocalDate.now())
                .build();
    }

    public void updateInfo(String name, String phone, String grade,
                          String schoolName, String parentName, String parentPhone) {
        if (name != null && !name.isBlank()) {
            this.user = new User(user.getEmail(), user.getPasswordHash(), name,
                                phone != null ? phone : user.getPhone(), Role.STUDENT) {};
        }
        if (grade != null) {
            this.grade = grade;
        }
        if (schoolName != null) {
            this.schoolName = schoolName;
        }
        if (parentName != null) {
            this.parentName = parentName;
        }
        if (parentPhone != null) {
            this.parentPhone = parentPhone;
        }
    }

    public void deactivate() {
        this.status = "INACTIVE";
        this.user.deactivate();
    }

    public void activate() {
        this.status = "ACTIVE";
        this.user.activate();
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    public String getStudentName() {
        return user.getName();
    }

    public String getStudentPhone() {
        return user.getPhone();
    }

    public String getStudentEmail() {
        return user.getEmail();
    }

    public Long getUserId() {
        return user.getId();
    }
}
