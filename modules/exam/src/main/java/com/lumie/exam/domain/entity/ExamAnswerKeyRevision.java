package com.lumie.exam.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "exam_answer_key_revisions")
public class ExamAnswerKeyRevision extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "exam_version_before", nullable = false, updatable = false)
    private Long examVersionBefore;

    @Column(name = "exam_version_after", nullable = false, updatable = false)
    private Long examVersionAfter;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_answers", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Map<String, String>> changedAnswers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Map<String, String>> changedFields;

    @Column(name = "regraded_result_count", nullable = false, updatable = false)
    private Integer regradedResultCount;

    @Column(name = "skipped_manual_override_count", nullable = false, updatable = false)
    private Integer skippedManualOverrideCount;

    @Column(name = "updated_submission_count", nullable = false, updatable = false)
    private Integer updatedSubmissionCount;

    protected ExamAnswerKeyRevision() {
    }

    private ExamAnswerKeyRevision(
            Exam exam,
            Long actorUserId,
            Long examVersionBefore,
            Long examVersionAfter,
            Map<String, AnswerChange> changedAnswers,
            Map<String, AnswerChange> changedFields,
            int regradedResultCount,
            int skippedManualOverrideCount,
            int updatedSubmissionCount) {
        this.exam = exam;
        this.actorUserId = actorUserId;
        this.examVersionBefore = examVersionBefore;
        this.examVersionAfter = examVersionAfter;
        this.changedAnswers = serialize(changedAnswers);
        this.changedFields = serialize(changedFields);
        this.regradedResultCount = regradedResultCount;
        this.skippedManualOverrideCount = skippedManualOverrideCount;
        this.updatedSubmissionCount = updatedSubmissionCount;
    }

    public static ExamAnswerKeyRevision create(
            Exam exam,
            Long actorUserId,
            Long examVersionBefore,
            Long examVersionAfter,
            Map<String, AnswerChange> changedAnswers,
            Map<String, AnswerChange> changedFields,
            int regradedResultCount,
            int skippedManualOverrideCount,
            int updatedSubmissionCount) {
        return new ExamAnswerKeyRevision(
                exam, actorUserId, examVersionBefore, examVersionAfter, changedAnswers, changedFields,
                regradedResultCount, skippedManualOverrideCount, updatedSubmissionCount);
    }

    private static Map<String, Map<String, String>> serialize(Map<String, AnswerChange> changes) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        changes.forEach((question, change) -> result.put(question,
                Map.of("before", change.before(), "after", change.after())));
        return Map.copyOf(result);
    }

    public Long getId() {
        return id;
    }

    public Exam getExam() {
        return exam;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public Long getExamVersionBefore() {
        return examVersionBefore;
    }

    public Long getExamVersionAfter() {
        return examVersionAfter;
    }

    public Map<String, Map<String, String>> getChangedAnswers() {
        return changedAnswers;
    }

    public Map<String, Map<String, String>> getChangedFields() {
        return changedFields;
    }

    public Integer getRegradedResultCount() {
        return regradedResultCount;
    }

    public Integer getSkippedManualOverrideCount() {
        return skippedManualOverrideCount;
    }

    public Integer getUpdatedSubmissionCount() {
        return updatedSubmissionCount;
    }

    public record AnswerChange(String before, String after) {}
}
