package com.lumie.exam.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.exam.domain.vo.JobStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "report_generation_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportGenerationJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "tenant_slug", nullable = false, length = 50)
    private String tenantSlug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "total_reports", nullable = false)
    private Integer totalReports;

    @Column(name = "processed_reports", nullable = false)
    private Integer processedReports;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "student_ids", columnDefinition = "jsonb")
    private List<Long> studentIds;

    @Column(name = "zip_file_key")
    private String zipFileKey;

    public static ReportGenerationJob create(Exam exam, String tenantSlug, List<Long> studentIds) {
        ReportGenerationJob job = new ReportGenerationJob();
        job.exam = exam;
        job.tenantSlug = tenantSlug;
        job.status = JobStatus.PENDING;
        job.totalReports = studentIds.size();
        job.processedReports = 0;
        job.successCount = 0;
        job.failCount = 0;
        job.studentIds = studentIds;
        return job;
    }

    public void setZipFileKey(String zipFileKey) {
        this.zipFileKey = zipFileKey;
    }
}
