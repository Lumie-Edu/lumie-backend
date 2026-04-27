package com.lumie.academy.application.dto.response.statistics;

public record AcademyDashboardResponse(
        int totalAcademies,
        int totalStudents,
        int activeStudents,
        int newEnrollmentsThisMonth,
        int newEnrollmentsLastMonth
) {
}
