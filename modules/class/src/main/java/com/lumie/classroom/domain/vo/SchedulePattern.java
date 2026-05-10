package com.lumie.classroom.domain.vo;

import java.util.List;

public record SchedulePattern(
        List<String> days,
        String startTime,
        String endTime
) {
}
