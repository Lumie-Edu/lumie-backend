package com.lumie.assignment.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "assignment_submissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Convert(converter = LongListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Long> attachments;

    @Column(name = "score")
    private Integer score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    @Builder
    private AssignmentSubmission(Long assignmentId, Long studentId, String content,
                                List<Long> attachments, Integer score, String feedback,
                                LocalDateTime submittedAt, LocalDateTime gradedAt,
                                SubmissionStatus status) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.content = content;
        this.attachments = attachments;
        this.score = score;
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
        this.submittedAt = LocalDateTime.now();
        this.status = SubmissionStatus.SUBMITTED;
    }

    public void grade(Integer score, String feedback) {
        this.score = score;
        this.feedback = feedback;
        this.gradedAt = LocalDateTime.now();
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
                throw new IllegalArgumentException("Failed to convert List<Long> to JSON", e);
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
                throw new IllegalArgumentException("Failed to convert JSON to List<Long>", e);
            }
        }
    }
}
