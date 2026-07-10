package com.lumie.assignment.domain.entity;

import com.lumie.assignment.domain.vo.AssignmentTargetType;
import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assignment_targets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "target_type", "target_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentTarget extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AssignmentTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Builder
    private AssignmentTarget(Long assignmentId, AssignmentTargetType targetType, Long targetId) {
        this.assignmentId = assignmentId;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public static AssignmentTarget create(Long assignmentId, AssignmentTargetType targetType, Long targetId) {
        return AssignmentTarget.builder()
                .assignmentId(assignmentId)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }
}
