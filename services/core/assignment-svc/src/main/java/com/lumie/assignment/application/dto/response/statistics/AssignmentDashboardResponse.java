package com.lumie.assignment.application.dto.response.statistics;

public record AssignmentDashboardResponse(
        int pending,
        int submitted,
        int graded,
        int returned
) {
}
