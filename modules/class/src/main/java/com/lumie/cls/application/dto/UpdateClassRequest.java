package com.lumie.cls.application.dto;

import com.lumie.cls.domain.vo.SchedulePattern;

public record UpdateClassRequest(
    String name,
    Long teacherId,
    SchedulePattern schedulePattern,
    String description
) {
}
