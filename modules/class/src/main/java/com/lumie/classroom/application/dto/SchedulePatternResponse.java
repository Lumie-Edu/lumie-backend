package com.lumie.classroom.application.dto;

import com.lumie.classroom.domain.vo.SchedulePattern;

import java.util.List;

public record SchedulePatternResponse(
        List<ScheduleSessionResponse> sessions
) {
    public static SchedulePatternResponse from(SchedulePattern vo) {
        if (vo == null) {
            return null;
        }
        return new SchedulePatternResponse(
                vo.sessions().stream().map(ScheduleSessionResponse::from).toList()
        );
    }

    public record ScheduleSessionResponse(
            String day,
            String startTime,
            String endTime
    ) {
        public static ScheduleSessionResponse from(SchedulePattern.ScheduleSession session) {
            return new ScheduleSessionResponse(session.day(), session.startTime(), session.endTime());
        }
    }
}
