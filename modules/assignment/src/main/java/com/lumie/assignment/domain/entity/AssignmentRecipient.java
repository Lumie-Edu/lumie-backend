package com.lumie.assignment.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assignment_recipients", uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentRecipient extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private Long assignmentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "source_provenance", nullable = false, updatable = false)
    private String sourceProvenance;

    private AssignmentRecipient(Long assignmentId, Long studentId) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.sourceProvenance = "EXACT";
    }

    public static AssignmentRecipient create(Long assignmentId, Long studentId) {
        return new AssignmentRecipient(assignmentId, studentId);
    }
}
