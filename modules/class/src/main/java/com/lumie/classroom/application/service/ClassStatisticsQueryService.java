package com.lumie.classroom.application.service;

import com.lumie.classroom.application.dto.response.statistics.ClassDashboardResponse;
import com.lumie.classroom.domain.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClassStatisticsQueryService {

    private final ClassRepository classRepository;

    public ClassDashboardResponse getDashboardStatistics() {
        log.debug("Getting class dashboard statistics");

        long totalClasses = classRepository.count();

        return new ClassDashboardResponse((int) totalClasses);
    }
}
