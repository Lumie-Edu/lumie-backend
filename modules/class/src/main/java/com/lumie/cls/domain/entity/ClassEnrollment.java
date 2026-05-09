package com.lumie.cls.domain.entity;

import com.lumie.cls.domain.vo.EnrollmentStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_enrollments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassEnrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "dropped_at")
    private LocalDateTime droppedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Builder
    private ClassEnrollment(ClassEntity classEntity, Long studentId, LocalDateTime enrolledAt,
                            LocalDateTime droppedAt, EnrollmentStatus status) {
        this.classEntity = classEntity;
        this.studentId = studentId;
        this.enrolledAt = enrolledAt;
        this.droppedAt = droppedAt;
        this.status = status != null ? status : EnrollmentStatus.ENROLLED;
    }

    public static ClassEnrollment create(ClassEntity classEntity, Long studentId) {
        return ClassEnrollment.builder()
                .classEntity(classEntity)
                .studentId(studentId)
                .enrolledAt(LocalDateTime.now())
                .status(EnrollmentStatus.ENROLLED)
                .build();
    }

    public void drop() {
        this.status = EnrollmentStatus.DROPPED;
        this.droppedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = EnrollmentStatus.COMPLETED;
    }
}
