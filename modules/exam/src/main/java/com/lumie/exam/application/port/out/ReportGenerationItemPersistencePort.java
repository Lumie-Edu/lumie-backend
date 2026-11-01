package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.ReportGenerationItem;
import com.lumie.exam.domain.vo.ReportGenerationItemStatus;

import java.util.List;
import java.util.Optional;

/** Application-owned report item persistence operations. */
public interface ReportGenerationItemPersistencePort {

    Optional<ReportGenerationItem> findByJobIdAndReportIndex(Long jobId, Integer reportIndex);
    Optional<ReportGenerationItem> findByJobIdAndReportIndexForUpdate(Long jobId, Integer reportIndex);
    List<ReportGenerationItem> findByJobIdOrderByReportIndexAsc(Long jobId);
    List<ReportGenerationItem> findByJobIdAndStatusOrderByReportIndexAsc(
            Long jobId, ReportGenerationItemStatus status);
    List<ReportGenerationItem> saveAll(Iterable<ReportGenerationItem> items);
}
