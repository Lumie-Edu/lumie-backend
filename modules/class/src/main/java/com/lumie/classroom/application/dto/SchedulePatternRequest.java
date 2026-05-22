package com.lumie.classroom.application.dto;

import com.lumie.classroom.domain.vo.SchedulePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SchedulePatternRequest(
        @NotEmpty(message = "At least one schedule day is required")
        List<String> days,

        @NotBlank(message = "Schedule start time is required")
        String startTime,

        @NotBlank(message = "Schedule end time is required")
        String endTime
) {
    public SchedulePattern toVo() {
        return new SchedulePattern(days, startTime, endTime);
    }
}
