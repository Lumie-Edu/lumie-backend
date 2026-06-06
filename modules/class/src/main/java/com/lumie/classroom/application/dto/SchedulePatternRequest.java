package com.lumie.classroom.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lumie.classroom.domain.vo.SchedulePattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SchedulePatternRequest(
        List<String> days,

        String startTime,

        String endTime,

        @Valid
        List<@NotNull(message = "Schedule session is required") ScheduleSessionRequest> sessions
) {
    public SchedulePattern toVo() {
        return new SchedulePattern(
                days,
                startTime,
                endTime,
                sessions == null ? null : sessions.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(ScheduleSessionRequest::toVo)
                        .toList()
        );
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Schedule must include either days/startTime/endTime or at least one session")
    public boolean isScheduleShapeValid() {
        return hasLegacyShape() || hasSessionShape();
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Schedule end time must be after start time")
    public boolean isScheduleTimeRangeValid() {
        if (hasLegacyShape() && !endTime.isBlank() && !startTime.isBlank()
                && endTime.compareTo(startTime) <= 0) {
            return false;
        }
        if (sessions == null) {
            return true;
        }
        return sessions.stream()
                .allMatch(session -> session != null && session.hasValidTimeRange());
    }

    private boolean hasLegacyShape() {
        return days != null && !days.isEmpty()
                && startTime != null && !startTime.isBlank()
                && endTime != null && !endTime.isBlank();
    }

    private boolean hasSessionShape() {
        return sessions != null && !sessions.isEmpty()
                && sessions.stream().allMatch(java.util.Objects::nonNull);
    }

    public record ScheduleSessionRequest(
            @NotBlank(message = "Schedule day is required")
            String day,

            @NotBlank(message = "Schedule start time is required")
            String startTime,

            @NotBlank(message = "Schedule end time is required")
            String endTime
    ) {
        SchedulePattern.ScheduleSession toVo() {
            return new SchedulePattern.ScheduleSession(day, startTime, endTime);
        }

        boolean hasValidTimeRange() {
            return startTime != null && endTime != null && endTime.compareTo(startTime) > 0;
        }
    }
}
