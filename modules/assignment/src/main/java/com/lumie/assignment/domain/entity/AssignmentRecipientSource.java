package com.lumie.assignment.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assignment_recipient_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentRecipientSource extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "assignment_recipient_id", nullable = false, updatable = false)
    private Long recipientId;

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private Long assignmentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "class_target_id", updatable = false)
    private Long classTargetId;

    @Column(name = "student_target_id", updatable = false)
    private Long studentTargetId;

    @Column(name = "source_provenance", nullable = false)
    private String sourceProvenance;

    private AssignmentRecipientSource(Long recipientId, Long assignmentId, Long studentId,
                                      Long classTargetId, Long studentTargetId) {
        this.recipientId = recipientId;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.classTargetId = classTargetId;
        this.studentTargetId = studentTargetId;
        this.sourceProvenance = "EXACT";
    }

    public static AssignmentRecipientSource forClass(Long recipientId, Long assignmentId,
                                                     Long studentId, Long classTargetId) {
        return new AssignmentRecipientSource(recipientId, assignmentId, studentId, classTargetId, null);
    }

    public static AssignmentRecipientSource forStudent(Long recipientId, Long assignmentId,
                                                       Long studentId, Long studentTargetId) {
        return new AssignmentRecipientSource(recipientId, assignmentId, studentId, null, studentTargetId);
    }

    public void confirmExact() {
        this.sourceProvenance = "EXACT";
    }
}
