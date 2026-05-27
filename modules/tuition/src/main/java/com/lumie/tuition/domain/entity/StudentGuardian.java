package com.lumie.tuition.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Join table mapping students to guardians within a tenant.
 * Composite PK: (student_id, guardian_id).
 * tenant_id is stored denormalized for RLS enforcement.
 */
@Entity
@Table(name = "student_guardians", indexes = {
        @Index(name = "idx_student_guardians_guardian_id", columnList = "guardian_id"),
        @Index(name = "idx_student_guardians_tenant_id", columnList = "tenant_id")
})
@IdClass(StudentGuardianId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentGuardian {

    @Id
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Id
    @Column(name = "guardian_id", nullable = false)
    private Long guardianId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Builder
    private StudentGuardian(Long studentId, Long guardianId, Long tenantId, Boolean isPrimary) {
        this.studentId = studentId;
        this.guardianId = guardianId;
        this.tenantId = tenantId;
        this.isPrimary = isPrimary != null ? isPrimary : false;
    }

    public static StudentGuardian create(Long studentId, Long guardianId, Long tenantId, boolean isPrimary) {
        return StudentGuardian.builder()
                .studentId(studentId)
                .guardianId(guardianId)
                .tenantId(tenantId)
                .isPrimary(isPrimary)
                .build();
    }

    public void designateAsPrimary() {
        this.isPrimary = true;
    }

    public void demoteFromPrimary() {
        this.isPrimary = false;
    }
}
