package com.lumie.admin.application.dto.response.statistics;

import java.util.Map;

public record AdminDashboardResponse(
        int totalStaff,
        Map<String, Integer> roleDistribution
) {
}
