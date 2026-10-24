package com.lumie.app;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.ReportCallbackRequest;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.application.service.ReportJobService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ReportGenerationItem;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ReportGenerationItemRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.exam.domain.vo.ReportGenerationItemStatus;
import com.lumie.file.api.FileObjectKeyFactory;
import com.lumie.student.api.StudentService;
import org.flywaydb.core.Flyway;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.DriverManager;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Tag("integration")
class ReportCallbackStateIntegrationTest extends FullContextTestBase {

    private static final long TENANT_ID = 890_101L;
    private static final String TENANT_SLUG = "report-callback-it";

    private final TransactionTemplate transactions;
    private final ExamRepository examRepository;
    private final ReportGenerationJobRepository jobRepository;
    private final ReportGenerationItemRepository itemRepository;
    private final ReportJobService reportJobService;
    private final JdbcTemplate jdbc;
    @MockitoBean
    private OmrStoragePort storagePort;
    @MockitoBean
    private StudentService studentService;

    ReportCallbackStateIntegrationTest(
            @Autowired TransactionTemplate transactions,
            @Autowired ExamRepository examRepository,
            @Autowired ReportGenerationJobRepository jobRepository,
            @Autowired ReportGenerationItemRepository itemRepository,
            @Autowired ReportJobService reportJobService,
            @Autowired JdbcTemplate jdbc) {
        this.transactions = transactions;
        this.examRepository = examRepository;
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.reportJobService = reportJobService;
        this.jdbc = jdbc;
    }

    @BeforeAll
    static void migrateAndSeedTenant() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("""
                    DO $$ BEGIN
                      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'lumie_app') THEN
                        CREATE ROLE lumie_app NOSUPERUSER NOBYPASSRLS;
                      END IF;
                      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'postgres') THEN
                        CREATE ROLE postgres NOSUPERUSER NOBYPASSRLS;
                      END IF;
                    END $$;
                    """);
        }
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .baselineVersion("0");
        configuration.getConfigurationExtension(PostgreSQLConfigurationExtension.class)
                .setTransactionalLock(false);
        configuration.load().migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO tenants (id, slug, name, status, plan_id)
                    VALUES (890101, 'report-callback-it', 'Report Callback IT', 'ACTIVE', 'BASIC')
                    ON CONFLICT (id) DO NOTHING
                    """);
        }
    }

    @Test
    void itemTransitionsAreTenantScopedAndDuplicateOutcomesDoNotRecount() {
        JobData data = createJob(List.of(991_001L, 991_002L, 991_003L));
        byte[] pdf = "%PDF-1.7\nreport".getBytes(StandardCharsets.US_ASCII);
        String fileKey = reportKey(data.examId(), 991_001L);
        given(storagePort.getObject(fileKey)).willReturn(pdf);

        withinTenant(() -> {
            ReportCallbackRequest success = callback(data, 991_001L, 0, true, null, fileKey, null);
            reportJobService.processCallback(success);
            reportJobService.processCallback(success);
            reportJobService.processCallback(callback(
                    data, 991_001L, 0, false, "conflicting failure", null, null));
            reportJobService.processCallback(callback(
                    data, 991_002L, 1, false, "render failed", null, null));
            reportJobService.processCallback(callback(
                    data, 991_002L, 1, false, "duplicate failure", null, null));

            var successItem = itemRepository.findByJobIdAndReportIndex(data.jobId(), 0).orElseThrow();
            var failedItem = itemRepository.findByJobIdAndReportIndex(data.jobId(), 1).orElseThrow();
            var job = jobRepository.findById(data.jobId()).orElseThrow();
            assertThat(successItem.getStatus()).isEqualTo(ReportGenerationItemStatus.SUCCESS);
            assertThat(successItem.getFileKey()).isEqualTo(fileKey);
            assertThat(successItem.getErrorMessage()).isNull();
            assertThat(failedItem.getStatus()).isEqualTo(ReportGenerationItemStatus.FAILED);
            assertThat(failedItem.getErrorMessage()).isEqualTo("render failed");
            assertThat(job.getProcessedReports()).isEqualTo(2);
            assertThat(job.getSuccessCount()).isEqualTo(1);
            assertThat(job.getFailCount()).isEqualTo(1);
            assertThat(job.getStatus()).isEqualTo(JobStatus.PROCESSING);
        });

        verify(storagePort, times(1)).getObject(fileKey);
    }

    @Test
    void concurrentDuplicateCallbacksCommitOneStateTransition() throws Exception {
        JobData data = createJob(List.of(994_001L, 994_002L));
        byte[] pdf = "%PDF-1.7\nconcurrent".getBytes(StandardCharsets.US_ASCII);
        String fileKey = reportKey(data.examId(), 994_001L);
        CountDownLatch bothReading = new CountDownLatch(2);
        given(storagePort.getObject(fileKey)).willAnswer(invocation -> {
            bothReading.countDown();
            assertThat(bothReading.await(10, TimeUnit.SECONDS)).isTrue();
            return pdf;
        });
        ReportCallbackRequest callback = callback(data, 994_001L, 0, true, null, fileKey, null);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> withinTenant(() -> reportJobService.processCallback(callback)));
            var second = executor.submit(() -> withinTenant(() -> reportJobService.processCallback(callback)));
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }

        withinTenant(() -> {
            var job = jobRepository.findById(data.jobId()).orElseThrow();
            var item = itemRepository.findByJobIdAndReportIndex(data.jobId(), 0).orElseThrow();
            assertThat(item.getStatus()).isEqualTo(ReportGenerationItemStatus.SUCCESS);
            assertThat(job.getProcessedReports()).isEqualTo(1);
            assertThat(job.getSuccessCount()).isEqualTo(1);
        });
        verify(storagePort, times(2)).getObject(fileKey);
    }

    @Test
    @Test
    void storageAndDatabaseFailuresLeaveItemPendingForRedelivery() {
        JobData data = createJob(List.of(992_001L, 992_002L, 992_003L, 992_004L));
        byte[] pdf = "%PDF-1.7\nretry".getBytes(StandardCharsets.US_ASCII);
        String encoded = Base64.getEncoder().encodeToString(pdf);
        String storageKey = reportKey(data.examId(), 992_001L);
        given(storagePort.putObject(storageKey, pdf, "application/pdf"))
                .willThrow(new IllegalStateException("temporary object-store failure"))
                .willReturn(storageKey);

        withinTenant(() -> {
            ReportCallbackRequest callback = callback(data, 992_001L, 0, true, null, null, encoded);
            assertThatThrownBy(() -> reportJobService.processCallback(callback))
                    .isInstanceOf(IllegalStateException.class);
            assertPending(data, 0, 0);
            reportJobService.processCallback(callback);
            assertSuccess(data, 0, 1);
        });

        String function = "fail_report_item_transition_it";
        String trigger = "trg_fail_report_item_transition_it";
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION public.fail_report_item_transition_it()
                RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.status <> OLD.status THEN
                        RAISE EXCEPTION 'simulated report item database failure';
                    END IF;
                    RETURN NEW;
                END $$
                """);
        jdbc.execute("CREATE TRIGGER " + trigger + " BEFORE UPDATE ON report_generation_items "
                + "FOR EACH ROW EXECUTE FUNCTION public." + function + "()");
        String databaseFailureKey = reportKey(data.examId(), 992_002L);
        given(storagePort.putObject(databaseFailureKey, pdf, "application/pdf"))
                .willReturn(databaseFailureKey);
        ReportCallbackRequest dbFailureCallback = callback(data, 992_002L, 1, true, null, null, encoded);
        try {
            withinTenant(() -> assertThatThrownBy(() -> reportJobService.processCallback(dbFailureCallback))
                    .isInstanceOf(RuntimeException.class));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS " + trigger + " ON report_generation_items");
            jdbc.execute("DROP FUNCTION IF EXISTS public." + function + "()");
        }

        withinTenant(() -> {
            assertPending(data, 1, 1);
            reportJobService.processCallback(dbFailureCallback);
            assertSuccess(data, 1, 2);
        });
        verify(storagePort, times(2)).putObject(databaseFailureKey, pdf, "application/pdf");
    }

    @Test
    void completedItemIsNotRecountedAndFailedZipCanBeRebuiltOnDownload() {
        JobData data = createJob(List.of(993_001L, 993_002L));
        byte[] pdf = "%PDF-1.7\nzip".getBytes(StandardCharsets.US_ASCII);
        byte[] zip = new byte[]{1, 2, 3, 4};
        String orphanFileKey = reportKey(data.examId(), 993_001L);
        String fileKey = reportKey(data.examId(), 993_002L);
        String zipKey = FileObjectKeyFactory.reportArchive(
                TenantContextHolder.toStorageTenantId(TENANT_SLUG), data.examId(), data.jobId());
        given(storagePort.getObject(fileKey)).willReturn(pdf);
        given(studentService.getStudentsByIds(eq(TENANT_SLUG), eq(List.of(993_002L))))
                .willReturn(new StudentService.StudentListData(
                        List.of(new StudentService.StudentData(993_002L, TENANT_ID, "학생", "010", true,
                                null, null)),
                        1, 0, 1));
        given(storagePort.putObject(eq(zipKey), any(byte[].class), eq("application/zip")))
                .willThrow(new IllegalStateException("temporary zip write failure"))
                .willReturn(zipKey);
        given(storagePort.getObject(zipKey)).willReturn(zip);

        withinTenant(() -> {
            // Simulate a success callback that uploaded its deterministic object
            // but lost the database race to a conflicting failure callback.
            reportJobService.processCallback(callback(
                    data, 993_001L, 0, false, "first terminal outcome", null, null));
            ReportCallbackRequest success = callback(data, 993_002L, 1, true, null, fileKey, null);
            reportJobService.processCallback(success);
            reportJobService.processCallback(success);

            var completed = jobRepository.findById(data.jobId()).orElseThrow();
            assertThat(completed.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(completed.getProcessedReports()).isEqualTo(2);
            assertThat(completed.getFailCount()).isEqualTo(1);
            assertThat(completed.getSuccessCount()).isEqualTo(1);
            assertThat(completed.getZipFileKey()).isNull();

            assertThat(reportJobService.getZipBytes(data.jobId())).containsExactly(zip);
            var rebuilt = jobRepository.findById(data.jobId()).orElseThrow();
            assertThat(rebuilt.getProcessedReports()).isEqualTo(2);
            assertThat(rebuilt.getFailCount()).isEqualTo(1);
            assertThat(rebuilt.getSuccessCount()).isEqualTo(1);
            assertThat(rebuilt.getZipFileKey()).isEqualTo(zipKey);
        });
        verify(storagePort, times(2)).putObject(eq(zipKey), any(byte[].class), eq("application/zip"));
        org.mockito.Mockito.verify(storagePort, org.mockito.Mockito.never()).getObject(orphanFileKey);
    }

    private JobData createJob(List<Long> studentIds) {
        return withinTenant(() -> transactions.execute(status -> {
            Exam exam = examRepository.saveAndFlush(Exam.create(
                    "Report callback integration test", ExamCategory.GRADED, GradingType.ABSOLUTE,
                    GradeScale.NINE_GRADE, 1, Map.of("1", "1"), Map.of("1", 100), Map.of("1", "단답"), null));
            ReportGenerationJob job = jobRepository.saveAndFlush(
                    ReportGenerationJob.create(exam, TENANT_SLUG, studentIds));
            List<ReportGenerationItem> items = java.util.stream.IntStream.range(0, studentIds.size())
                    .mapToObj(index -> ReportGenerationItem.pending(
                            job.getId(), exam.getId(), index, studentIds.get(index)))
                    .toList();
            itemRepository.saveAllAndFlush(items);
            return new JobData(job.getId(), exam.getId(), studentIds.size());
        }));
    }

    private void assertPending(JobData data, int index, int expectedProcessed) {
        assertThat(itemRepository.findByJobIdAndReportIndex(data.jobId(), index).orElseThrow().getStatus())
                .isEqualTo(ReportGenerationItemStatus.PENDING);
        var job = jobRepository.findById(data.jobId()).orElseThrow();
        assertThat(job.getProcessedReports()).isEqualTo(expectedProcessed);
        assertThat(job.getSuccessCount()).isEqualTo(expectedProcessed);
    }

    private void assertSuccess(JobData data, int index, int expectedProcessed) {
        assertThat(itemRepository.findByJobIdAndReportIndex(data.jobId(), index).orElseThrow().getStatus())
                .isEqualTo(ReportGenerationItemStatus.SUCCESS);
        var job = jobRepository.findById(data.jobId()).orElseThrow();
        assertThat(job.getProcessedReports()).isEqualTo(expectedProcessed);
        assertThat(job.getSuccessCount()).isEqualTo(expectedProcessed);
    }

    private static String reportKey(Long examId, Long studentId) {
        return FileObjectKeyFactory.temporaryReport(
                TenantContextHolder.toStorageTenantId(TENANT_SLUG), examId, studentId);
    }

    private static ReportCallbackRequest callback(
            JobData job, Long studentId, int index, boolean success,
            String error, String fileKey, String reportBytes) {
        return new ReportCallbackRequest(
                job.jobId(), job.examId(), studentId, TENANT_SLUG, index, job.totalReports(),
                success, error, fileKey, reportBytes);
    }

    private static void withinTenant(Runnable action) {
        TenantContextHolder.withinContext(TENANT_SLUG, TENANT_ID, action);
    }

    private static <T> T withinTenant(java.util.function.Supplier<T> action) {
        return TenantContextHolder.withinContext(TENANT_SLUG, TENANT_ID, action);
    }

    private record JobData(Long jobId, Long examId, int totalReports) {
    }
}
