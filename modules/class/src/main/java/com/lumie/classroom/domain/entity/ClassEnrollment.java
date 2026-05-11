package com.lumie.classroom.domain.entity;

import com.lumie.classroom.domain.vo.EnrollmentStatus;
import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "class_enrollments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassEnrollment extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "dropped_at")
    private Instant droppedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Builder
    private ClassEnrollment(ClassEntity classEntity, Long studentId, Instant enrolledAt,
                            Instant droppedAt, EnrollmentStatus status) {
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
                .enrolledAt(Instant.now())
                .status(EnrollmentStatus.ENROLLED)
                .build();
    }

    public void drop() {
        this.status = EnrollmentStatus.DROPPED;
        this.droppedAt = Instant.now();
    }

    public void complete() {
        this.status = EnrollmentStatus.COMPLETED;
    }
}
