package com.lumie.cls.application.dto;

import com.lumie.cls.domain.vo.SchedulePattern;

import java.time.LocalDate;

public record UpdateClassRequest(
    String name,
    String subject,
    Long teacherId,
    Integer maxStudents,
    Long tuitionAmount,
    SchedulePattern schedulePattern,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    String description
) {
}
