package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.ReportGenerationItemPersistencePort;
import com.lumie.exam.domain.entity.ReportGenerationItem;
import com.lumie.exam.domain.vo.ReportGenerationItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportGenerationItemPersistenceAdapter implements ReportGenerationItemPersistencePort {

    private final ReportGenerationItemJpaRepository jpaRepository;

    @Override public Optional<ReportGenerationItem> findByJobIdAndReportIndex(Long jobId, Integer reportIndex) {
        return jpaRepository.findByJobIdAndReportIndex(jobId, reportIndex);
    }
    @Override public Optional<ReportGenerationItem> findByJobIdAndReportIndexForUpdate(Long jobId, Integer index) {
        return jpaRepository.findByJobIdAndReportIndexForUpdate(jobId, index);
    }
    @Override public List<ReportGenerationItem> findByJobIdOrderByReportIndexAsc(Long jobId) {
        return jpaRepository.findByJobIdOrderByReportIndexAsc(jobId);
    }
    @Override public List<ReportGenerationItem> findByJobIdAndStatusOrderByReportIndexAsc(
            Long jobId, ReportGenerationItemStatus status) {
        return jpaRepository.findByJobIdAndStatusOrderByReportIndexAsc(jobId, status);
    }
    @Override public List<ReportGenerationItem> saveAll(Iterable<ReportGenerationItem> items) {
        return jpaRepository.saveAll(items);
    }
}
