package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.response.statistics.AcademyDashboardResponse;
import com.lumie.academy.domain.repository.AcademyRepository;
import com.lumie.academy.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AcademyStatisticsQueryService {

    private final AcademyRepository academyRepository;
    private final StudentRepository studentRepository;

    public AcademyDashboardResponse getDashboardStatistics() {
        log.debug("Getting academy dashboard statistics");

        long totalAcademies = academyRepository.count();
        long activeStudents = studentRepository.countByIsActive(true);
        long inactiveStudents = studentRepository.countByIsActive(false);
        long totalStudents = activeStudents + inactiveStudents;

        // Capture once to avoid race across midnight between YearMonth.now() and LocalDate.now()
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        long newEnrollmentsThisMonth = studentRepository.countByCreatedAtBetween(
                currentMonth.atDay(1).atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        long newEnrollmentsLastMonth = studentRepository.countByCreatedAtBetween(
                previousMonth.atDay(1).atStartOfDay(),
                currentMonth.atDay(1).atStartOfDay()
        );

        return new AcademyDashboardResponse(
                Math.toIntExact(totalAcademies),
                Math.toIntExact(totalStudents),
                Math.toIntExact(activeStudents),
                Math.toIntExact(newEnrollmentsThisMonth),
                Math.toIntExact(newEnrollmentsLastMonth)
        );
    }
}
