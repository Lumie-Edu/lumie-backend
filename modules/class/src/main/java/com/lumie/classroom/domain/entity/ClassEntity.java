package com.lumie.classroom.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.classroom.domain.exception.ClassErrorCode;
import com.lumie.classroom.domain.vo.SchedulePattern;
import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.common.exception.BusinessException;
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
public class ClassEntity extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Convert(converter = SchedulePatternConverter.class)
    @Column(name = "schedule_pattern", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private SchedulePattern schedulePattern;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder
    private ClassEntity(String name, Long teacherId,
                        SchedulePattern schedulePattern, String description) {
        this.name = name;
        this.teacherId = teacherId;
        this.schedulePattern = schedulePattern;
        this.description = description;
    }

    public static ClassEntity create(String name, Long teacherId,
                                     SchedulePattern schedulePattern, String description) {
        return ClassEntity.builder()
                .name(name)
                .teacherId(teacherId)
                .schedulePattern(schedulePattern)
                .description(description)
                .build();
    }

    public void updateInfo(String name, Long teacherId,
                           SchedulePattern schedulePattern, String description) {
        this.name = name;
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
                throw new BusinessException(ClassErrorCode.JSON_SERIALIZATION_FAILED, e);
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
                throw new BusinessException(ClassErrorCode.JSON_SERIALIZATION_FAILED, e);
            }
        }
    }
}
