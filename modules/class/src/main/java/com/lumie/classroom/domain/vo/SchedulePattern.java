package com.lumie.classroom.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SchedulePattern(List<ScheduleSession> sessions) {

    public SchedulePattern {
        sessions = sessions == null ? List.of() : List.copyOf(sessions);
    }

    public record ScheduleSession(
            String day,
            String startTime,
            String endTime
    ) {
    }
}
