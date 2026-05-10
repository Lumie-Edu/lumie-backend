package com.lumie.classroom.application.dto;

import com.lumie.classroom.domain.vo.SchedulePattern;

public record UpdateClassRequest(
    String name,
    Long teacherId,
    SchedulePattern schedulePattern,
    String description
) {
}
