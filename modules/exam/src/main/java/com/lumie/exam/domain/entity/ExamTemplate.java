package com.lumie.exam.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "exam_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamTemplate extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ExamCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_type", length = 20)
    private GradingType gradingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_scale", length = 20)
    private GradeScale gradeScale;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_scores", nullable = false, columnDefinition = "jsonb")
    private Map<String, Integer> questionScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_types", columnDefinition = "jsonb")
    private Map<String, String> questionTypes;

    @Column(name = "pass_score")
    private Integer passScore;

    @Builder
    private ExamTemplate(String name, ExamCategory category, GradingType gradingType, GradeScale gradeScale,
                         Integer totalQuestions,
                         Map<String, Integer> questionScores, Map<String, String> questionTypes,
                         Integer passScore) {
        this.name = name;
        this.category = category;
        this.gradingType = gradingType;
        this.gradeScale = gradeScale;
        this.totalQuestions = totalQuestions;
        this.questionScores = questionScores;
        this.questionTypes = questionTypes;
        this.passScore = passScore;
    }

    public static ExamTemplate create(String name, ExamCategory category, GradingType gradingType, GradeScale gradeScale,
                                      Integer totalQuestions,
                                      Map<String, Integer> questionScores, Map<String, String> questionTypes,
                                      Integer passScore) {
        ExamCategory effectiveCategory = category != null ? category : ExamCategory.GRADED;
        return ExamTemplate.builder()
                .name(name)
                .category(effectiveCategory)
                .gradingType(effectiveCategory == ExamCategory.GRADED ? gradingType : null)
                .gradeScale(effectiveCategory == ExamCategory.GRADED ? gradeScale : null)
                .totalQuestions(totalQuestions)
                .questionScores(questionScores)
                .questionTypes(questionTypes)
                .passScore(effectiveCategory == ExamCategory.PASS_FAIL ? passScore : null)
                .build();
    }

    public void update(String name, ExamCategory category, GradingType gradingType, GradeScale gradeScale,
                       Integer totalQuestions,
                       Map<String, Integer> questionScores, Map<String, String> questionTypes,
                       Integer passScore) {
        boolean categoryChanged = category != null && category != this.category;
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (category != null) {
            this.category = category;
        }
        if (gradingType != null) {
            this.gradingType = gradingType;
        }
        if (gradeScale != null) {
            this.gradeScale = gradeScale;
        }
        if (totalQuestions != null) {
            this.totalQuestions = totalQuestions;
        }
        if (questionScores != null) {
            this.questionScores = questionScores;
        }
        if (questionTypes != null) {
            this.questionTypes = questionTypes;
        }
        if (passScore != null) {
            this.passScore = passScore;
        }
        if (categoryChanged && this.category == ExamCategory.PASS_FAIL) {
            this.gradingType = null;
            this.gradeScale = null;
        } else if (categoryChanged) {
            this.passScore = null;
        }
    }

    public int calculateTotalPossibleScore() {
        return questionScores.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
}
