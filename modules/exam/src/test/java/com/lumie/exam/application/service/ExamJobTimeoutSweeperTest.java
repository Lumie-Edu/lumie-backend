package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ReportGenerationItem;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.repository.ReportGenerationItemRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.exam.domain.vo.ReportGenerationItemStatus;
import com.lumie.exam.test.ExamFixtures;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamJobTimeoutSweeperTest {

    @Mock
    private TenantService tenantService;
    @Mock
    private ExamJobTimeoutSweeper.Tx sweeperTx;
    @InjectMocks
    private ExamJobTimeoutSweeper sweeper;

    @Mock
    private OmrGradingJobRepository omrJobRepository;
    @Mock
    private ReportGenerationJobRepository reportJobRepository;
    @Mock
    private ReportGenerationItemRepository itemRepository;

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void restoresPreviousTenantAndMdcWhenSecondSweepFails() {
        TenantContextHolder.setTenant("outer");
        TenantContextHolder.setTenantId(99L);
        MDC.put("tenantSlug", "outer");
        MDC.put("tenantId", "99");
        when(tenantService.listActiveTenants()).thenReturn(List.of(
                new TenantService.TenantData(7L, "inner", "Inner", TenantStatus.ACTIVE, null, "", "")));
        when(sweeperTx.sweepOmrJobs(any(Instant.class))).thenAnswer(invocation -> {
            assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("inner");
            assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(7L);
            assertThat(MDC.get("tenantSlug")).isEqualTo("inner");
            return 1;
        });
        when(sweeperTx.sweepReportJobs(any(Instant.class)))
                .thenThrow(new IllegalStateException("test failure"));

        sweeper.sweepStaleJobs();

        assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("outer");
        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(99L);
        assertThat(MDC.get("tenantSlug")).isEqualTo("outer");
        assertThat(MDC.get("tenantId")).isEqualTo("99");
    }

    @Test
    void staleReportJobFailsPendingItemsAndCountsThem() {
        ReportGenerationJob job = reportJob();
        ReportGenerationItem first = ReportGenerationItem.pending(1L, 10L, 0, 20L);
        ReportGenerationItem second = ReportGenerationItem.pending(1L, 10L, 1, 21L);
        given(reportJobRepository.findStaleOpenJobsForUpdate(
                eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING)), any(Instant.class)))
                .willReturn(List.of(job));
        given(itemRepository.findByJobIdAndStatusOrderByReportIndexAsc(
                1L, ReportGenerationItemStatus.PENDING)).willReturn(List.of(first, second));
        given(reportJobRepository.failStaleJob(1L, 2)).willReturn(1);

        int updated = tx().sweepReportJobs(Instant.now());

        assertThat(updated).isEqualTo(1);
        assertThat(first.getStatus()).isEqualTo(ReportGenerationItemStatus.FAILED);
        assertThat(second.getStatus()).isEqualTo(ReportGenerationItemStatus.FAILED);
        assertThat(first.getErrorMessage()).isEqualTo("REPORT_JOB_TIMEOUT");
        verify(reportJobRepository).failStaleJob(1L, 2);
    }

    @Test
    private ExamJobTimeoutSweeper.Tx tx() {
        return new ExamJobTimeoutSweeper.Tx(omrJobRepository, reportJobRepository, itemRepository);
    }

    private static ReportGenerationJob reportJob() {
        Exam exam = ExamFixtures.aGradedExam();
        ReflectionTestUtils.setField(exam, "id", 10L);
        ReportGenerationJob job = ReportGenerationJob.create(exam, "test-tenant", List.of(20L, 21L));
        ReflectionTestUtils.setField(job, "id", 1L);
        return job;
    }
}
