package com.lumie.student.application.dto.response;

public record DropoutSummaryResponse(
        long thisMonthDropouts,
        double thisMonthDropoutRate,
        long lastMonthDropouts
) {}
