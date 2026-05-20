package com.lumie.staff.application.dto.response.statistics;

import java.util.Map;

public record StaffDashboardResponse(
        int totalStaff,
        Map<String, Integer> roleDistribution
) {
}
