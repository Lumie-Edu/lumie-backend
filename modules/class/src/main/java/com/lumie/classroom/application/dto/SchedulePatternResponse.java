package com.lumie.classroom.application.dto;

import com.lumie.classroom.domain.vo.SchedulePattern;

import java.util.List;

public record SchedulePatternResponse(
        List<String> days,
        String startTime,
        String endTime
) {
    public static SchedulePatternResponse from(SchedulePattern vo) {
        if (vo == null) {
            return null;
        }
        return new SchedulePatternResponse(vo.days(), vo.startTime(), vo.endTime());
    }
}
