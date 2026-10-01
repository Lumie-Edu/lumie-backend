package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.TenantData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
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
 *
 * <p>The {@code @Transactional} sweep methods are extracted to
 * {@link Tx} so the per-tenant loop invokes them through the Spring proxy.
 * Calling {@code this.sweepOmrJobs(cutoff)} would bypass the proxy and the
 * annotation would silently do nothing — the historical bug that left
 * the sweeper logging "Executing an update/delete query" every 10 minutes
 * (NEXT_SESSION.md follow-up to #22).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamJobTimeoutSweeper {

    private static final Duration STALE_THRESHOLD = Duration.ofHours(2);

    private final TenantService tenantService;
    private final Tx tx;

    @Scheduled(fixedDelay = 600_000)
    @SchedulerLock(name = "examJobTimeoutSweeper",
            lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void sweepStaleJobs() {
        Instant cutoff = Instant.now().minus(STALE_THRESHOLD);
        List<TenantData> tenants = tenantService.listActiveTenants();

        int totalOmr = 0;
        int totalReport = 0;
        for (TenantData tenant : tenants) {
            String previousMdcSlug = MDC.get("tenantSlug");
            String previousMdcId = MDC.get("tenantId");
            try {
                MDC.put("tenantSlug", tenant.slug());
                MDC.put("tenantId", String.valueOf(tenant.id()));
                totalOmr += TenantContextHolder.withinContext(tenant.slug(), tenant.id(), () -> {
                    return tx.sweepOmrJobs(cutoff);
                });
                totalReport += TenantContextHolder.withinContext(tenant.slug(), tenant.id(), () -> {
                    return tx.sweepReportJobs(cutoff);
                });
            } catch (Exception e) {
                log.error("Failed to sweep stale jobs for tenant {}: {}", tenant.slug(), e.getMessage());
            } finally {
                if (previousMdcSlug == null) {
                    MDC.remove("tenantSlug");
                } else {
                    MDC.put("tenantSlug", previousMdcSlug);
                }
                if (previousMdcId == null) {
                    MDC.remove("tenantId");
                } else {
                    MDC.put("tenantId", previousMdcId);
                }
            }
        }

        if (totalOmr + totalReport > 0) {
            log.warn("Stale job sweep: {} OMR + {} report jobs marked FAILED across {} tenants",
                    totalOmr, totalReport, tenants.size());
        }
    }

    @Component
    @RequiredArgsConstructor
    static class Tx {
        private final OmrGradingJobRepository omrGradingJobRepository;
        private final ReportGenerationJobRepository reportGenerationJobRepository;

        @Transactional
        public int sweepOmrJobs(Instant cutoff) {
            return omrGradingJobRepository.markStaleAsFailed(cutoff);
        }

        @Transactional
        public int sweepReportJobs(Instant cutoff) {
            return reportGenerationJobRepository.markStaleAsFailed(cutoff);
        }
    }
}
