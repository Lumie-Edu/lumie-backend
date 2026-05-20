package com.lumie.staff.application.service;

import com.lumie.staff.application.dto.response.statistics.StaffDashboardResponse;
import com.lumie.staff.domain.repository.StaffRepository;
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
public class StaffStatisticsQueryService {

    private final StaffRepository staffRepository;

    public StaffDashboardResponse getDashboardStatistics() {
        log.debug("Getting staff dashboard statistics");

        long totalStaff = staffRepository.countByIsActive(true);

        List<Object[]> roleRows = staffRepository.countActiveGroupedByRole();
        Map<String, Integer> roleDistribution = new HashMap<>();
        for (Object[] row : roleRows) {
            String role = row[0] != null ? row[0].toString() : "UNKNOWN";
            int count = ((Number) row[1]).intValue();
            roleDistribution.put(role, count);
        }

        return new StaffDashboardResponse(
                Math.toIntExact(totalStaff),
                Map.copyOf(roleDistribution)
        );
    }
}
