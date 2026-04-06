package com.lumie.cls.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.cls.domain.vo.ClassStatus;
import com.lumie.cls.domain.vo.SchedulePattern;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

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

    @Column(name = "max_students", nullable = false)
    private int maxStudents;

    @Column(name = "tuition_amount", nullable = false)
    private long tuitionAmount;

    @Convert(converter = SchedulePatternConverter.class)
    @Column(name = "schedule_pattern", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private SchedulePattern schedulePattern;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClassStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder
    private ClassEntity(Long academyId, String name, String subject, Long teacherId,
                        int maxStudents, long tuitionAmount, SchedulePattern schedulePattern,
                        LocalDate startDate, LocalDate endDate, ClassStatus status, String description) {
        this.academyId = academyId;
        this.name = name;
        this.subject = subject;
        this.teacherId = teacherId;
        this.maxStudents = maxStudents;
        this.tuitionAmount = tuitionAmount;
        this.schedulePattern = schedulePattern;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status != null ? status : ClassStatus.PENDING;
        this.description = description;
    }

    public static ClassEntity create(Long academyId, String name, String subject, Long teacherId,
                                     int maxStudents, long tuitionAmount, SchedulePattern schedulePattern,
                                     LocalDate startDate, LocalDate endDate, String description) {
        return ClassEntity.builder()
                .academyId(academyId)
                .name(name)
                .subject(subject)
                .teacherId(teacherId)
                .maxStudents(maxStudents)
                .tuitionAmount(tuitionAmount)
                .schedulePattern(schedulePattern)
                .startDate(startDate)
                .endDate(endDate)
                .status(ClassStatus.PENDING)
                .description(description)
                .build();
    }

    public void updateInfo(String name, String subject, Long teacherId, int maxStudents,
                           long tuitionAmount, SchedulePattern schedulePattern,
                           LocalDate startDate, LocalDate endDate, String description) {
        this.name = name;
        this.subject = subject;
        this.teacherId = teacherId;
        this.maxStudents = maxStudents;
        this.tuitionAmount = tuitionAmount;
        this.schedulePattern = schedulePattern;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    public void close() {
        this.status = ClassStatus.CLOSED;
    }

    public void activate() {
        this.status = ClassStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == ClassStatus.ACTIVE;
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
