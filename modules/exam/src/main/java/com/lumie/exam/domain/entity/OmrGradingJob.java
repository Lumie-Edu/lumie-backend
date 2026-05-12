package com.lumie.exam.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.exam.domain.vo.JobStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "omr_grading_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OmrGradingJob extends TenantScopedEntity {

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

    @Column(name = "total_images", nullable = false)
    private Integer totalImages;

    @Column(name = "processed_images", nullable = false)
    private Integer processedImages;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount;

    @Column(name = "saved_count", nullable = false)
    private Integer savedCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_keys", columnDefinition = "jsonb")
    private List<String> imageKeys;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "results", columnDefinition = "jsonb")
    private List<Map<String, Object>> results;

    public static OmrGradingJob create(Exam exam, String tenantSlug, int totalImages, List<String> imageKeys) {
        OmrGradingJob job = new OmrGradingJob();
        job.exam = exam;
        job.tenantSlug = tenantSlug;
        job.status = JobStatus.PENDING;
        job.totalImages = totalImages;
        job.processedImages = 0;
        job.successCount = 0;
        job.failCount = 0;
        job.savedCount = 0;
        job.imageKeys = imageKeys;
        return job;
    }

    public void startProcessing() {
        this.status = JobStatus.PROCESSING;
    }

    public void incrementProcessed(boolean success, boolean saved) {
        this.processedImages++;
        if (success) {
            this.successCount++;
            if (saved) {
                this.savedCount++;
            }
        } else {
            this.failCount++;
        }
    }

    public void complete(List<Map<String, Object>> results) {
        this.status = JobStatus.COMPLETED;
        this.results = results;
    }

    public void fail(List<Map<String, Object>> results) {
        this.status = JobStatus.FAILED;
        this.results = results;
    }
}
