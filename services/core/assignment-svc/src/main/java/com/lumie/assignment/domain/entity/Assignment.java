package com.lumie.assignment.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;
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
@Table(name = "assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private AssignmentType type;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "max_score")
    private Integer maxScore;

    @Convert(converter = LongListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Long> attachments;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status;

    @Builder
    private Assignment(Long classId, Long academyId, String title, String description,
                       AssignmentType type, LocalDateTime dueDate, Integer maxScore,
                       List<Long> attachments, Long createdBy, AssignmentStatus status) {
        this.classId = classId;
        this.academyId = academyId;
        this.title = title;
        this.description = description;
        this.type = type != null ? type : AssignmentType.HOMEWORK;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
        this.attachments = attachments;
        this.createdBy = createdBy;
        this.status = status != null ? status : AssignmentStatus.ACTIVE;
    }

    public static Assignment create(Long classId, Long academyId, String title, String description,
                                    AssignmentType type, LocalDateTime dueDate, Integer maxScore,
                                    List<Long> attachments, Long createdBy) {
        return Assignment.builder()
                .classId(classId)
                .academyId(academyId)
                .title(title)
                .description(description)
                .type(type != null ? type : AssignmentType.HOMEWORK)
                .dueDate(dueDate)
                .maxScore(maxScore)
                .attachments(attachments)
                .createdBy(createdBy)
                .status(AssignmentStatus.ACTIVE)
                .build();
    }

    public void updateInfo(String title, String description, AssignmentType type,
                           LocalDateTime dueDate, Integer maxScore, List<Long> attachments) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
        this.attachments = attachments;
    }

    public void close() {
        this.status = AssignmentStatus.CLOSED;
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
