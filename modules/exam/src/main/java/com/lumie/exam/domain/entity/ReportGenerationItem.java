package com.lumie.exam.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.exam.domain.vo.ReportGenerationItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_generation_items", uniqueConstraints =
        @UniqueConstraint(columnNames = {"tenant_id", "job_id", "report_index"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportGenerationItem extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "job_id", nullable = false, updatable = false)
    private Long jobId;

    @Column(name = "exam_id", nullable = false, updatable = false)
    private Long examId;

    @Column(name = "report_index", nullable = false, updatable = false)
    private Integer reportIndex;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReportGenerationItemStatus status;

    @Column(name = "file_key", length = 500)
    private String fileKey;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    private ReportGenerationItem(Long jobId, Long examId, int reportIndex, Long studentId) {
        this.jobId = jobId;
        this.examId = examId;
        this.reportIndex = reportIndex;
        this.studentId = studentId;
        this.status = ReportGenerationItemStatus.PENDING;
    }

    public static ReportGenerationItem pending(
            Long jobId, Long examId, int reportIndex, Long studentId) {
        return new ReportGenerationItem(jobId, examId, reportIndex, studentId);
    }

    public boolean completeSuccessfully(String fileKey) {
        if (status != ReportGenerationItemStatus.PENDING) {
            return false;
        }
        this.status = ReportGenerationItemStatus.SUCCESS;
        this.fileKey = fileKey;
        this.errorMessage = null;
        return true;
    }

    public boolean completeWithFailure(String errorMessage) {
        if (status != ReportGenerationItemStatus.PENDING) {
            return false;
        }
        this.status = ReportGenerationItemStatus.FAILED;
        this.fileKey = null;
        this.errorMessage = errorMessage;
        return true;
    }
}
