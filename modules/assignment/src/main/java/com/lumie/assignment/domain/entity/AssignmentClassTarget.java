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
@Table(name = "assignment_class_targets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "class_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentClassTarget extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private Long assignmentId;

    @Column(name = "class_id", nullable = false, updatable = false)
    private Long classId;
}
