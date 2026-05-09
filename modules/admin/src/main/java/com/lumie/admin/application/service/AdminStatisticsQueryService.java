package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.response.statistics.AdminDashboardResponse;
import com.lumie.admin.domain.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminStatisticsQueryService {

    private final AdminRepository adminRepository;

    public AdminDashboardResponse getDashboardStatistics() {
        log.debug("Getting admin dashboard statistics");

        long totalStaff = adminRepository.countByIsActive(true);

        List<Object[]> roleRows = adminRepository.countActiveGroupedByRole();
        Map<String, Integer> roleDistribution = new HashMap<>();
        for (Object[] row : roleRows) {
            String role = row[0] != null ? row[0].toString() : "UNKNOWN";
            int count = ((Number) row[1]).intValue();
            roleDistribution.put(role, count);
        }

        return new AdminDashboardResponse(
                Math.toIntExact(totalStaff),
                Map.copyOf(roleDistribution)
        );
    }
}
