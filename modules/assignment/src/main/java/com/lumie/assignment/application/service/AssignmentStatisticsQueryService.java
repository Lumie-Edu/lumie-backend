package com.lumie.assignment.application.service;

import com.lumie.common.util.ReportingTime;
import com.lumie.assignment.application.dto.response.statistics.AssignmentDashboardResponse;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AssignmentStatisticsQueryService {

    private static final ZoneId REPORTING_ZONE = ReportingTime.ZONE;

    private final AssignmentSubmissionRepository submissionRepository;

    public AssignmentDashboardResponse getDashboardStatistics() {
        log.debug("Getting assignment dashboard statistics");

        return toDashboardResponse(submissionRepository.countGroupedByStatus());
    }

    public AssignmentDashboardResponse getDashboardStatistics(int months) {
        int window = Math.max(1, Math.min(months, 24));
        YearMonth firstMonth = YearMonth.now(REPORTING_ZONE).minusMonths(window - 1L);
        Instant from = firstMonth.atDay(1).atStartOfDay(REPORTING_ZONE).toInstant();
        return toDashboardResponse(submissionRepository.countGroupedByStatusSince(from));
    }

    private AssignmentDashboardResponse toDashboardResponse(List<Object[]> rows) {
        Map<SubmissionStatus, Integer> statusCounts = new HashMap<>();
        for (Object[] row : rows) {
            SubmissionStatus status = (SubmissionStatus) row[0];
            int count = ((Number) row[1]).intValue();
            statusCounts.put(status, count);
        }

        return new AssignmentDashboardResponse(
                statusCounts.getOrDefault(SubmissionStatus.PENDING, 0),
                statusCounts.getOrDefault(SubmissionStatus.SUBMITTED, 0),
                statusCounts.getOrDefault(SubmissionStatus.GRADED, 0),
                statusCounts.getOrDefault(SubmissionStatus.RETURNED, 0)
        );
    }
}
