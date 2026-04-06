package com.lumie.cls.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.cls.domain.vo.SchedulePattern;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "classes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "subject", length = 100)
    private String subject;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Convert(converter = SchedulePatternConverter.class)
    @Column(name = "schedule_pattern", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private SchedulePattern schedulePattern;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder
    private ClassEntity(Long academyId, String name, String subject, Long teacherId,
                        SchedulePattern schedulePattern, String description) {
        this.academyId = academyId;
        this.name = name;
        this.subject = subject;
        this.teacherId = teacherId;
        this.schedulePattern = schedulePattern;
        this.description = description;
    }

    public static ClassEntity create(Long academyId, String name, String subject, Long teacherId,
                                     SchedulePattern schedulePattern, String description) {
        return ClassEntity.builder()
                .academyId(academyId)
                .name(name)
                .subject(subject)
                .teacherId(teacherId)
                .schedulePattern(schedulePattern)
                .description(description)
                .build();
    }

    public void updateInfo(String name, String subject, Long teacherId,
                           SchedulePattern schedulePattern, String description) {
        this.name = name;
        this.subject = subject;
        this.teacherId = teacherId;
        this.schedulePattern = schedulePattern;
        this.description = description;
    }

    @Converter
    static class SchedulePatternConverter implements AttributeConverter<SchedulePattern, String> {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(SchedulePattern attribute) {
            if (attribute == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to convert SchedulePattern to JSON", e);
            }
        }

        @Override
        public SchedulePattern convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(dbData, SchedulePattern.class);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to convert JSON to SchedulePattern", e);
            }
        }
    }
}
