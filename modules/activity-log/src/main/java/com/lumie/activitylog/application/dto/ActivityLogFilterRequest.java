package com.lumie.activitylog.application.dto;

import java.time.LocalDate;

public record ActivityLogFilterRequest(
        Long actorId,
        String entityType,
        LocalDate startDate,
        LocalDate endDate,
        String search
) {
}
