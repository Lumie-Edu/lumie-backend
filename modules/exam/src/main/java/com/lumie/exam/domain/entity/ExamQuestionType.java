package com.lumie.exam.domain.entity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "exam_question_types",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_exam_question_types_tenant_name",
                columnNames = {"tenant_id", "name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamQuestionType extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Builder
    private ExamQuestionType(String name, Integer displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public static ExamQuestionType create(String name, Integer displayOrder) {
        return ExamQuestionType.builder()
                .name(name.trim())
                .displayOrder(displayOrder)
                .build();
    }
}
