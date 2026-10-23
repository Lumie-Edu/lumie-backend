package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ReportGenerationItem;
import com.lumie.exam.domain.vo.ReportGenerationItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportGenerationItemRepository extends JpaRepository<ReportGenerationItem, Long> {

    Optional<ReportGenerationItem> findByJobIdAndReportIndex(Long jobId, Integer reportIndex);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item from ReportGenerationItem item
            where item.jobId = :jobId and item.reportIndex = :reportIndex
            """)
    Optional<ReportGenerationItem> findByJobIdAndReportIndexForUpdate(
            @Param("jobId") Long jobId,
            @Param("reportIndex") Integer reportIndex);

    List<ReportGenerationItem> findByJobIdOrderByReportIndexAsc(Long jobId);

    List<ReportGenerationItem> findByJobIdAndStatusOrderByReportIndexAsc(
            Long jobId, ReportGenerationItemStatus status);
}
