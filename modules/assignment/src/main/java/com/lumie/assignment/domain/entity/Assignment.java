package com.lumie.assignment.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;
import com.lumie.common.domain.TenantScopedEntity;
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

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private AssignmentType type;

    @Column(name = "due_date")
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

    @Builder
    private Assignment(Long classId, String title, String description,
                       AssignmentType type, Instant dueDate, Integer maxScore,
                       List<Long> attachments, AssignmentStatus status) {
        this.classId = classId;
        this.title = title;
        this.description = description;
        this.type = type != null ? type : AssignmentType.HOMEWORK;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
        this.attachments = attachments;
        this.status = status != null ? status : AssignmentStatus.ACTIVE;
    }

    public static Assignment create(Long classId, String title, String description,
                                    AssignmentType type, Instant dueDate, Integer maxScore,
                                    List<Long> attachments) {
        return Assignment.builder()
                .classId(classId)
                .title(title)
                .description(description)
                .type(type != null ? type : AssignmentType.HOMEWORK)
                .dueDate(dueDate)
                .maxScore(maxScore)
                .attachments(attachments)
                .status(AssignmentStatus.ACTIVE)
                .build();
    }

    public void updateInfo(String title, String description, AssignmentType type,
                           Instant dueDate, Integer maxScore, List<Long> attachments) {
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
