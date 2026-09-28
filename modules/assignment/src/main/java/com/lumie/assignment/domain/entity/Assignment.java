package com.lumie.assignment.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.EvaluationMode;
import com.lumie.assignment.domain.vo.SubmissionMode;
import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.common.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "max_score")
    private Integer maxScore;

    @Convert(converter = LongListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Long> attachments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_mode", nullable = false, length = 30)
    private SubmissionMode submissionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_mode", nullable = false, length = 30)
    private EvaluationMode evaluationMode;

    @Column(name = "linked_exam_id", updatable = false)
    private Long linkedExamId;

    @Column(name = "show_score", nullable = false)
    private boolean showScore;

    @Column(name = "show_pass_fail", nullable = false)
    private boolean showPassFail;

    @Column(name = "show_grade", nullable = false)
    private boolean showGrade;

    @Column(name = "show_question_results", nullable = false)
    private boolean showQuestionResults;

    @Column(name = "show_correct_answers", nullable = false)
    private boolean showCorrectAnswers;

    @Builder
    private Assignment(Long classId, Instant dueDate, Integer maxScore,
                       List<Long> attachments, AssignmentStatus status,
                       SubmissionMode submissionMode, EvaluationMode evaluationMode,
                       Long linkedExamId, Boolean showScore, Boolean showPassFail,
                       Boolean showGrade, Boolean showQuestionResults, Boolean showCorrectAnswers) {
        if (dueDate == null) {
            throw new BusinessException(AssignmentErrorCode.DUE_DATE_REQUIRED);
        }
        this.classId = classId;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
        this.attachments = attachments;
        this.status = status != null ? status : AssignmentStatus.ACTIVE;
        this.submissionMode = submissionMode != null ? submissionMode : SubmissionMode.EXAM_MANUAL;
        this.evaluationMode = evaluationMode != null ? evaluationMode : EvaluationMode.EXAM_AUTO;
        this.linkedExamId = linkedExamId;
        this.showScore = showScore != null ? showScore : true;
        this.showPassFail = showPassFail != null ? showPassFail : true;
        this.showGrade = showGrade != null ? showGrade : false;
        this.showQuestionResults = showQuestionResults != null ? showQuestionResults : true;
        this.showCorrectAnswers = showCorrectAnswers != null ? showCorrectAnswers : false;
    }

    public static Assignment create(Long classId, Instant dueDate, Integer maxScore, List<Long> attachments) {
        return create(classId, dueDate, maxScore, attachments,
                AssignmentStatus.ACTIVE, SubmissionMode.EXAM_MANUAL, EvaluationMode.EXAM_AUTO, null,
                true, true, false, true, false);
    }

    public static Assignment create(Long classId, Instant dueDate, Integer maxScore, List<Long> attachments,
                                    AssignmentStatus status,
                                    SubmissionMode submissionMode, EvaluationMode evaluationMode,
                                    Long linkedExamId, Boolean showScore, Boolean showPassFail,
                                    Boolean showGrade, Boolean showQuestionResults, Boolean showCorrectAnswers) {
        return Assignment.builder()
                .classId(classId)
                .dueDate(dueDate)
                .maxScore(maxScore)
                .attachments(attachments)
                .status(status)
                .submissionMode(submissionMode)
                .evaluationMode(evaluationMode)
                .linkedExamId(linkedExamId)
                .showScore(showScore)
                .showPassFail(showPassFail)
                .showGrade(showGrade)
                .showQuestionResults(showQuestionResults)
                .showCorrectAnswers(showCorrectAnswers)
                .build();
    }

    public void updateInfo(Instant dueDate, Integer maxScore, List<Long> attachments) {
        if (dueDate == null) {
            throw new BusinessException(AssignmentErrorCode.DUE_DATE_REQUIRED);
        }
        this.dueDate = dueDate;
        this.maxScore = maxScore;
        this.attachments = attachments;
    }

    public void updateWorkflow(Long classId, SubmissionMode submissionMode, EvaluationMode evaluationMode,
                               Boolean showScore, Boolean showPassFail,
                               Boolean showGrade, Boolean showQuestionResults, Boolean showCorrectAnswers,
                               AssignmentStatus status) {
        this.classId = classId;
        this.submissionMode = submissionMode;
        this.evaluationMode = evaluationMode;
        this.showScore = Boolean.TRUE.equals(showScore);
        this.showPassFail = Boolean.TRUE.equals(showPassFail);
        this.showGrade = Boolean.TRUE.equals(showGrade);
        this.showQuestionResults = Boolean.TRUE.equals(showQuestionResults);
        this.showCorrectAnswers = Boolean.TRUE.equals(showCorrectAnswers);
        this.status = status;
    }

    public void close() {
        this.status = AssignmentStatus.CLOSED;
    }

    public boolean closeIfOverdue(Instant now) {
        if (this.status == AssignmentStatus.ACTIVE && !this.dueDate.isAfter(now)) {
            close();
            return true;
        }
        return false;
    }

    public void activate() {
        this.status = AssignmentStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == AssignmentStatus.ACTIVE;
    }

    @Converter
    static class LongListConverter implements AttributeConverter<List<Long>, String> {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<Long> attribute) {
            if (attribute == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new BusinessException(AssignmentErrorCode.JSON_SERIALIZATION_FAILED, e);
            }
        }

        @Override
        public List<Long> convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<Long>>() {});
            } catch (JsonProcessingException e) {
                throw new BusinessException(AssignmentErrorCode.JSON_SERIALIZATION_FAILED, e);
            }
        }
    }
}
