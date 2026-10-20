package com.lumie.exam.application.service;

import com.lumie.common.test.TenantContextExtension;
import com.lumie.exam.application.dto.request.ReportCallbackRequest;
import com.lumie.exam.application.event.ReportBatchGenerationRequestedEvent;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.exam.test.ExamFixtures;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
class ReportJobServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ReportGenerationJobRepository jobRepository;
    @Mock
    private OmrStoragePort storagePort;
    @Mock
    private CallbackDeduplicationPort callbackDeduplication;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private StudentService studentService;

    private ReportJobService service;

    @BeforeEach
    void setUp() {
        ReportJobService.Tx tx = new ReportJobService.Tx(jobRepository, studentService);
        service = new ReportJobService(
                examRepository,
                jobRepository,
                storagePort,
                callbackDeduplication,
                eventPublisher,
                studentService,
                tx
        );
    }

    @Test
    void createBatchJob_publishesChunkedBatchEvents() {
        Exam exam = ExamFixtures.aGradedExam();
        ReflectionTestUtils.setField(exam, "id", 10L);
        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(jobRepository.save(any(ReportGenerationJob.class))).willAnswer(invocation -> {
            ReportGenerationJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", 1L);
            return job;
        });
        List<Long> studentIds = java.util.stream.LongStream.rangeClosed(1, 65)
                .boxed()
                .toList();

        Long jobId = service.createBatchJob(10L, studentIds);

        assertThat(jobId).isEqualTo(1L);
        ArgumentCaptor<ReportBatchGenerationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(ReportBatchGenerationRequestedEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(3)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(ReportBatchGenerationRequestedEvent::startIndex)
                .containsExactly(0, 30, 60);
        assertThat(eventCaptor.getAllValues())
                .extracting(event -> event.studentIds().size())
                .containsExactly(30, 30, 5);
        assertThat(eventCaptor.getAllValues())
                .allSatisfy(event -> {
                    assertThat(event.jobId()).isEqualTo(1L);
                    assertThat(event.examId()).isEqualTo(10L);
                    assertThat(event.totalReports()).isEqualTo(65);
                    assertThat(event.tenantSlug()).isEqualTo("test-tenant");
                });
    }

    @Test
    void processCallback_storesOnlyPdfReports() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
        given(callbackDeduplication.markIfAbsent(anyString())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);

        service.processCallback(successCallback(Base64.getEncoder().encodeToString(pdfBytes)));

        verify(storagePort).putObject(
                "tmp/test-tenant/reports/10/99.pdf",
                pdfBytes,
                ReportFileFormat.PDF_CONTENT_TYPE
        );
        verify(jobRepository).incrementSuccess(1L);
        verify(jobRepository, never()).incrementFail(1L);
    }

    @Test
    void processCallback_failedStorageAttemptCanBeRetried() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
        String key = "report:cb:1:0";
        Set<String> seenKeys = new HashSet<>();
        given(callbackDeduplication.markIfAbsent(key)).willAnswer(invocation -> seenKeys.add(key));
        doAnswer(invocation -> { seenKeys.remove(invocation.getArgument(0)); return null; })
                .when(callbackDeduplication).forget(key);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);
        given(storagePort.putObject("tmp/test-tenant/reports/10/99.pdf", pdfBytes, ReportFileFormat.PDF_CONTENT_TYPE))
                .willThrow(new IllegalStateException("temporary storage failure"))
                .willReturn("tmp/test-tenant/reports/10/99.pdf");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.processCallback(
                        successCallback(Base64.getEncoder().encodeToString(pdfBytes))))
                .isInstanceOf(IllegalStateException.class);
        service.processCallback(successCallback(Base64.getEncoder().encodeToString(pdfBytes)));

        verify(callbackDeduplication).forget(key);
        verify(storagePort, org.mockito.Mockito.times(2)).putObject(
                "tmp/test-tenant/reports/10/99.pdf", pdfBytes, ReportFileFormat.PDF_CONTENT_TYPE);
        verify(jobRepository).incrementSuccess(1L);
        verify(jobRepository, never()).incrementFail(1L);
    }

    @Test
    void processCallback_duplicateSuccessfulDeliveryIsCountedOnce() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
        given(callbackDeduplication.markIfAbsent("report:cb:1:0")).willReturn(true, false);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);

        ReportCallbackRequest callback = successCallback(Base64.getEncoder().encodeToString(pdfBytes));
        service.processCallback(callback);
        service.processCallback(callback);

        verify(storagePort).putObject(
                "tmp/test-tenant/reports/10/99.pdf", pdfBytes, ReportFileFormat.PDF_CONTENT_TYPE);
        verify(jobRepository).incrementSuccess(1L);
        verify(callbackDeduplication, never()).forget("report:cb:1:0");
    }

    @Test
    void processCallback_acceptsPreUploadedPdfFileKey() {
        given(callbackDeduplication.markIfAbsent(anyString())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.pdf"))
                .willReturn("%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII));

        service.processCallback(successCallbackWithFileKey("tmp/test-tenant/reports/10/99.pdf"));

        verify(storagePort, never()).putObject(anyString(), any(), anyString());
        verify(jobRepository).incrementSuccess(1L);
        verify(jobRepository, never()).incrementFail(1L);
    }

    @Test
    void processCallback_rejectsUnexpectedFileKey() {
        given(callbackDeduplication.markIfAbsent(anyString())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);

        service.processCallback(successCallbackWithFileKey("tmp/other/reports/10/99.pdf"));

        verify(storagePort, never()).putObject(anyString(), any(), anyString());
        verify(jobRepository).incrementFail(1L);
        verify(jobRepository, never()).incrementSuccess(1L);
    }

    @Test
    void processCallback_rejectsUnreadableFileKey() {
        given(callbackDeduplication.markIfAbsent(anyString())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.pdf"))
                .willReturn(new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00});

        service.processCallback(successCallbackWithFileKey("tmp/test-tenant/reports/10/99.pdf"));

        verify(storagePort, never()).putObject(anyString(), any(), anyString());
        verify(jobRepository).incrementFail(1L);
        verify(jobRepository, never()).incrementSuccess(1L);
    }

    @Test
    void processCallback_recordsFailureWhenFileKeyReadFails() {
        given(callbackDeduplication.markIfAbsent(anyString())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.pdf"))
                .willThrow(new IllegalStateException("missing object"));

        service.processCallback(successCallbackWithFileKey("tmp/test-tenant/reports/10/99.pdf"));

        verify(jobRepository).incrementFail(1L);
        verify(jobRepository, never()).incrementSuccess(1L);
    }

    @Test
    void processCallback_marksNonPdfPayloadAsFailed() {
        byte[] jpgBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00};
        given(callbackDeduplication.markIfAbsent(anyString())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);

        service.processCallback(successCallback(Base64.getEncoder().encodeToString(jpgBytes)));

        verify(storagePort, never()).putObject(anyString(), any(), anyString());
        verify(jobRepository).incrementFail(1L);
        verify(jobRepository, never()).incrementSuccess(1L);
    }

    @Test
    void getZipBytes_fallsBackToLegacyJpgObjectsDuringMigration() throws Exception {
        ReportGenerationJob job = completedJob();
        given(jobRepository.findById(1L)).willReturn(Optional.of(job));
        given(studentService.getStudentsByIds(eq("test-tenant"), eq(List.of(99L))))
                .willReturn(new StudentService.StudentListData(
                        List.of(new StudentService.StudentData(99L, 7L, "강다은", "010", true, null, null)),
                        1,
                        0,
                        1
                ));
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.pdf"))
                .willThrow(new IllegalStateException("missing pdf"));
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.jpg"))
                .willReturn(new byte[]{1, 2, 3});
        given(storagePort.putObject(eq("tmp/test-tenant/reports/10/batch_1.zip"), any(), eq("application/zip")))
                .willReturn("tmp/test-tenant/reports/10/batch_1.zip");
        doAnswer(invocation -> {
            ReflectionTestUtils.setField(job, "zipFileKey", invocation.getArgument(1));
            ReflectionTestUtils.setField(job, "zipExpiresAt", invocation.getArgument(2));
            return null;
        }).when(jobRepository).setZipFileKey(eq(1L), anyString(), any(Instant.class));

        service.getZipBytes(1L);

        ArgumentCaptor<byte[]> zipCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(storagePort).putObject(eq("tmp/test-tenant/reports/10/batch_1.zip"), zipCaptor.capture(), eq("application/zip"));
        assertThat(firstZipEntryName(zipCaptor.getValue())).isEqualTo("강다은_테스트_시험.jpg");
    }

    @Test
    void getZipBytes_rejectsExpiredZipBeforeReadingOrRebuilding() {
        ReportGenerationJob job = completedJob();
        ReflectionTestUtils.setField(job, "zipFileKey", "tmp/test-tenant/reports/10/batch_1.zip");
        ReflectionTestUtils.setField(job, "zipExpiresAt", Instant.now().minusSeconds(1));
        given(jobRepository.findById(1L)).willReturn(Optional.of(job));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getZipBytes(1L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class)
                .hasMessageContaining("expired")
                .extracting("errorCode").isEqualTo(ExamErrorCode.REPORT_ZIP_EXPIRED);

        verify(storagePort, never()).getObject(anyString());
        verify(studentService, never()).getStudentsByIds(anyString(), any());
        verify(storagePort, never()).putObject(anyString(), any(), anyString());
    }

    @Test
    void expiredReportZipErrorUsesGoneStatus() {
        assertThat(ExamErrorCode.REPORT_ZIP_EXPIRED.getStatus()).isEqualTo(410);
    }

    @Test
    void getZipBytes_rejectsLegacyZipWithoutExpiryInsteadOfCreatingANewWindow() {
        ReportGenerationJob job = completedJob();
        ReflectionTestUtils.setField(job, "zipFileKey", "tmp/test-tenant/reports/10/batch_1.zip");
        given(jobRepository.findById(1L)).willReturn(Optional.of(job));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getZipBytes(1L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        verify(storagePort, never()).getObject(anyString());
        verify(storagePort, never()).putObject(anyString(), any(), anyString());
    }

    @Test
    void getZipBytes_assignsExpiryFromZipStorageTime() throws Exception {
        ReportGenerationJob job = completedJob();
        given(jobRepository.findById(1L)).willReturn(Optional.of(job));
        given(studentService.getStudentsByIds(eq("test-tenant"), eq(List.of(99L))))
                .willReturn(new StudentService.StudentListData(
                        List.of(new StudentService.StudentData(99L, 7L, "강다은", "010", true, null, null)),
                        1, 0, 1));
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.pdf"))
                .willReturn("%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII));
        given(storagePort.putObject(eq("tmp/test-tenant/reports/10/batch_1.zip"), any(), eq("application/zip")))
                .willReturn("tmp/test-tenant/reports/10/batch_1.zip");
        doAnswer(invocation -> {
            ReflectionTestUtils.setField(job, "zipFileKey", invocation.getArgument(1));
            ReflectionTestUtils.setField(job, "zipExpiresAt", invocation.getArgument(2));
            return null;
        }).when(jobRepository).setZipFileKey(eq(1L), anyString(), any(Instant.class));
        given(storagePort.getObject("tmp/test-tenant/reports/10/batch_1.zip"))
                .willReturn(new byte[]{1, 2, 3});

        Instant before = Instant.now();
        service.getZipBytes(1L);
        Instant after = Instant.now();

        Instant expiry = job.getZipExpiresAt();
        assertThat(expiry).isNotNull();
        assertThat(expiry).isBetween(before.plusSeconds(24 * 60 * 60), after.plusSeconds(24 * 60 * 60));
        verify(jobRepository).setZipFileKey(eq(1L), eq("tmp/test-tenant/reports/10/batch_1.zip"), any(Instant.class));
    }

    @Test
    void getJobStatus_exposesPersistedZipExpiry() {
        ReportGenerationJob job = completedJob();
        Instant expiresAt = Instant.parse("2026-10-21T00:00:00Z");
        ReflectionTestUtils.setField(job, "zipFileKey", "tmp/test-tenant/reports/10/batch_1.zip");
        ReflectionTestUtils.setField(job, "zipExpiresAt", expiresAt);
        given(jobRepository.findById(1L)).willReturn(Optional.of(job));

        var status = service.getJobStatus(1L);

        assertThat(status.expiresAt()).isEqualTo(expiresAt);
        assertThat(status.zipFileKey()).isEqualTo("tmp/test-tenant/reports/10/batch_1.zip");
    }

    @Test
    void getZipBytes_doesNotStoreAnEmptyZipWhenAllIntermediateFilesAreMissing() {
        ReportGenerationJob job = completedJob();
        given(jobRepository.findById(1L)).willReturn(Optional.of(job));
        given(studentService.getStudentsByIds(eq("test-tenant"), eq(List.of(99L))))
                .willReturn(new StudentService.StudentListData(
                        List.of(new StudentService.StudentData(99L, 7L, "강다은", "010", true, null, null)),
                        1, 0, 1));
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.pdf"))
                .willThrow(new IllegalStateException("missing pdf"));
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.jpg"))
                .willThrow(new IllegalStateException("missing legacy jpg"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getZipBytes(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incomplete report ZIP")
                .hasMessageContaining("found 0");

        verify(storagePort, never()).putObject(anyString(), any(), anyString());
    }

    @Test
    void getZipBytes_doesNotStoreAPartialZipWhenAnIntermediateReportIsMissing() {
        ReportGenerationJob job = completedJob();
        ReflectionTestUtils.setField(job, "studentIds", List.of(99L, 100L));
        ReflectionTestUtils.setField(job, "successCount", 2);
        given(jobRepository.findById(1L)).willReturn(Optional.of(job));
        given(studentService.getStudentsByIds(eq("test-tenant"), eq(List.of(99L, 100L))))
                .willReturn(new StudentService.StudentListData(
                        List.of(
                                new StudentService.StudentData(99L, 7L, "강다은", "010", true, null, null),
                                new StudentService.StudentData(100L, 7L, "김다은", "010", true, null, null)),
                        2, 0, 2));
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.pdf"))
                .willReturn("%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII));
        given(storagePort.getObject("tmp/test-tenant/reports/10/100.pdf"))
                .willThrow(new IllegalStateException("missing pdf"));
        given(storagePort.getObject("tmp/test-tenant/reports/10/100.jpg"))
                .willThrow(new IllegalStateException("missing legacy jpg"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getZipBytes(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incomplete report ZIP")
                .hasMessageContaining("expected 2 entries, found 1");

        verify(storagePort, never()).putObject(anyString(), any(), anyString());
    }

    @Test
    void getZipBytes_recoveryDoesNotExtendTheOriginalExpiry() throws Exception {
        ReportGenerationJob job = completedJob();
        Instant originalExpiry = Instant.now().plusSeconds(3600);
        ReflectionTestUtils.setField(job, "zipFileKey", "tmp/test-tenant/reports/10/batch_1.zip");
        ReflectionTestUtils.setField(job, "zipExpiresAt", originalExpiry);
        given(jobRepository.findById(1L)).willReturn(Optional.of(job));
        given(storagePort.getObject("tmp/test-tenant/reports/10/batch_1.zip"))
                .willThrow(new IllegalStateException("temporarily unavailable"))
                .willReturn(new byte[]{1, 2, 3});
        given(studentService.getStudentsByIds(eq("test-tenant"), eq(List.of(99L))))
                .willReturn(new StudentService.StudentListData(
                        List.of(new StudentService.StudentData(99L, 7L, "강다은", "010", true, null, null)),
                        1, 0, 1));
        given(storagePort.getObject("tmp/test-tenant/reports/10/99.pdf"))
                .willReturn("%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII));
        given(storagePort.putObject(eq("tmp/test-tenant/reports/10/batch_1.zip"), any(), eq("application/zip")))
                .willReturn("tmp/test-tenant/reports/10/batch_1.zip");

        service.getZipBytes(1L);

        assertThat(job.getZipExpiresAt()).isEqualTo(originalExpiry);
        verify(jobRepository, never()).setZipFileKey(any(), anyString(), any(Instant.class));
    }

    private static ReportCallbackRequest successCallback(String reportBytes) {
        return new ReportCallbackRequest(
                1L,
                10L,
                99L,
                "test-tenant",
                0,
                1,
                true,
                null,
                null,
                reportBytes
        );
    }

    private static ReportCallbackRequest successCallbackWithFileKey(String fileKey) {
        return new ReportCallbackRequest(
                1L,
                10L,
                99L,
                "test-tenant",
                0,
                1,
                true,
                null,
                fileKey,
                null
        );
    }

    private static ReportGenerationJob completedJob() {
        Exam exam = ExamFixtures.aGradedExam();
        ReflectionTestUtils.setField(exam, "id", 10L);
        ReportGenerationJob job = ReportGenerationJob.create(exam, "test-tenant", List.of(99L));
        ReflectionTestUtils.setField(job, "id", 1L);
        ReflectionTestUtils.setField(job, "status", JobStatus.COMPLETED);
        ReflectionTestUtils.setField(job, "successCount", 1);
        return job;
    }

    private static String firstZipEntryName(byte[] zipBytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();
            return entry == null ? null : entry.getName();
        }
    }
}
