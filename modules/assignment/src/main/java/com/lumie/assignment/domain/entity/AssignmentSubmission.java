package com.lumie.assignment.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.assignment.domain.vo.SubmissionStatus;
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
import java.util.Map;

@Entity
@Table(name = "assignment_submissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentSubmission extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Convert(converter = LongListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Long> attachments;

    @Convert(converter = StringMapConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", columnDefinition = "jsonb")
    private Map<String, String> answers;

    @Column(name = "exam_result_id")
    private Long examResultId;

    @Column(name = "score")
    private Integer score;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "grade_value", length = 30)
    private String gradeValue;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    @Builder
    private AssignmentSubmission(Long assignmentId, Long studentId, String content,
                                List<Long> attachments, Map<String, String> answers,
                                Long examResultId, Integer score, Boolean passed,
                                String gradeValue, String feedback, Instant submittedAt,
                                Instant gradedAt, SubmissionStatus status) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.content = content;
        this.attachments = attachments;
        this.answers = answers;
        this.examResultId = examResultId;
        this.score = score;
        this.passed = passed;
        this.gradeValue = gradeValue;
        this.feedback = feedback;
        this.submittedAt = submittedAt;
        this.gradedAt = gradedAt;
        this.status = status != null ? status : SubmissionStatus.PENDING;
    }

    public static AssignmentSubmission create(Long assignmentId, Long studentId) {
        return AssignmentSubmission.builder()
                .assignmentId(assignmentId)
                .studentId(studentId)
                .status(SubmissionStatus.PENDING)
                .build();
    }

    public void submit(String content, List<Long> attachments) {
        this.content = content;
        this.attachments = attachments;
        this.answers = null;
        this.examResultId = null;
        this.submittedAt = Instant.now();
        this.gradedAt = null;
        this.score = null;
        this.passed = null;
        this.gradeValue = null;
        this.status = SubmissionStatus.SUBMITTED;
    }

    public void grade(Integer score, String feedback) {
        this.score = score;
        this.feedback = feedback;
        this.gradedAt = Instant.now();
        this.status = SubmissionStatus.GRADED;
    }

    public void gradePassFail(Boolean passed, String feedback) {
        this.passed = passed;
        this.feedback = feedback;
        this.gradedAt = Instant.now();
        this.status = SubmissionStatus.GRADED;
    }

    public void gradeValue(String gradeValue, String feedback) {
        this.gradeValue = gradeValue;
        this.feedback = feedback;
        this.gradedAt = Instant.now();
        this.status = SubmissionStatus.GRADED;
    }

    public void submitExam(Map<String, String> answers, Long examResultId,
                           Integer score, Boolean passed, String gradeValue) {
        this.content = null;
        this.attachments = null;
        this.answers = answers;
        this.examResultId = examResultId;
        this.score = score;
        this.passed = passed;
        this.gradeValue = gradeValue;
        Instant now = Instant.now();
        this.submittedAt = now;
        this.gradedAt = now;
        this.status = SubmissionStatus.GRADED;
    }

    public void returnToStudent(String feedback) {
        this.feedback = feedback;
        this.status = SubmissionStatus.RETURNED;
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

    @Converter
    static class StringMapConverter implements AttributeConverter<Map<String, String>, String> {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(Map<String, String> attribute) {
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
        public Map<String, String> convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(dbData, new TypeReference<Map<String, String>>() {});
            } catch (JsonProcessingException e) {
                throw new BusinessException(AssignmentErrorCode.JSON_SERIALIZATION_FAILED, e);
            }
        }
    }
}
