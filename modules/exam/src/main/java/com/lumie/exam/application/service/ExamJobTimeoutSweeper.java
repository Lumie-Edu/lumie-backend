package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Marks long-running {@code PROCESSING} OMR/report jobs as {@code FAILED}
 * when their worker pod crashed without completing the callback.
 *
 * <p>Without this sweeper, a job whose worker died mid-grading stays in
 * {@code PROCESSING} forever — the frontend status poll never resolves and
 * the user can't retry.
 *
 * <p>Cutoff is the job's {@code created_at} (not {@code updated_at}) because
 * the per-image increment SQL bypasses Hibernate dirty-checking and never
 * refreshes {@code updated_at}. Two hours is generous enough that a slow
 * legitimate batch (1000 images on a busy worker) won't be killed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamJobTimeoutSweeper {

    private static final Duration STALE_THRESHOLD = Duration.ofHours(2);

    private final TenantService tenantService;
    private final OmrGradingJobRepository omrGradingJobRepository;
    private final ReportGenerationJobRepository reportGenerationJobRepository;

    @Scheduled(fixedDelay = 600_000)
    @SchedulerLock(name = "examJobTimeoutSweeper",
            lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void sweepStaleJobs() {
        Instant cutoff = Instant.now().minus(STALE_THRESHOLD);
        List<String> tenantSlugs = tenantService.listActiveTenantSlugs();

        int totalOmr = 0;
        int totalReport = 0;
        for (String slug : tenantSlugs) {
            try {
                TenantContextHolder.setTenant(slug);
                totalOmr += sweepOmrJobs(cutoff);
                totalReport += sweepReportJobs(cutoff);
            } catch (Exception e) {
                log.error("Failed to sweep stale jobs for tenant {}: {}", slug, e.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        }

        if (totalOmr + totalReport > 0) {
            log.warn("Stale job sweep: {} OMR + {} report jobs marked FAILED across {} tenants",
                    totalOmr, totalReport, tenantSlugs.size());
        }
    }

    @Transactional
    protected int sweepOmrJobs(Instant cutoff) {
        return omrGradingJobRepository.markStaleAsFailed(cutoff);
    }

    @Transactional
    protected int sweepReportJobs(Instant cutoff) {
        return reportGenerationJobRepository.markStaleAsFailed(cutoff);
    }
}
