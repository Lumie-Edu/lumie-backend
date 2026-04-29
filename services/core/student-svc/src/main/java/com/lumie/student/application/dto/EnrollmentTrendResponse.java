package com.lumie.student.application.dto;

import java.util.List;

public record EnrollmentTrendResponse(List<MonthlyEnrollment> months) {

    public record MonthlyEnrollment(
            String month,
            long newCount,
            long cumulativeCount
    ) {}
}
