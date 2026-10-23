package com.lumie.exam.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.api.SortFieldAllowlistInterceptor;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.service.ExamExportService;
import com.lumie.exam.application.service.OmrSubmissionService;
import com.lumie.exam.application.service.OmrUploadTelemetryService;
import com.lumie.exam.application.service.ResultCommandService;
import com.lumie.exam.application.service.ResultQueryService;
import com.lumie.exam.application.dto.request.OmrUploadTelemetryRequest;
import com.lumie.exam.application.dto.request.OmrPresignRequest;
import com.lumie.exam.application.dto.request.OmrBatchConfirmRequest;
import com.lumie.exam.application.dto.response.OmrJobResponse;
import com.lumie.exam.application.dto.request.UpdateQuestionAnswerRequest;
import com.lumie.exam.application.service.StudentGradeQueryService;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.common.idempotency.IdempotencyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResultController 단위 테스트")
class ResultControllerTest {

    private static final String TEST_ROLE_HEADER = "X-Test-Role";
    private static final String TEST_TENANT_HEADER = "X-Test-Tenant";

    @Mock private ResultCommandService resultCommandService;
    @Mock private ResultQueryService resultQueryService;
    @Mock private OmrSubmissionService omrSubmissionService;
    @Mock private OmrStoragePort omrStoragePort;
    @Mock private ExamExportService examExportService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private OmrUploadTelemetryService omrUploadTelemetryService;

    private ResultController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new ResultController(
            resultCommandService,
            resultQueryService,
            omrSubmissionService,
            omrStoragePort,
            examExportService,
            idempotencyService,
            omrUploadTelemetryService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new SortHandlerMethodArgumentResolver())
            .addInterceptors(new SortFieldAllowlistInterceptor())
            .addFilters(new TestRequestContextFilter())
            .build();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("exportExamResultsCsv: 강사 이상이면 CSV 스트리밍을 연결한다")
    void exportExamResultsCsv_allowsInstructorAndDelegates() throws Exception {
        TenantContextHolder.setTenant("test-academy");
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        Sort sort = Sort.by(Sort.Direction.DESC, StudentGradeQueryService.SORT_FIELD_TOTAL_SCORE);
        String expectedDate = LocalDate.now(com.lumie.common.util.ReportingTime.ZONE).format(DateTimeFormatter.BASIC_ISO_DATE);
        ExamExportService.CsvExport csvExport = new ExamExportService.CsvExport(List.of(), List.of());
        org.mockito.BDDMockito.given(examExportService.prepareCsvForExam(55L, 9L, true, false, sort))
            .willReturn(csvExport);

        ResponseEntity<StreamingResponseBody> response = controller.exportExamResultsCsv(
            55L,
            9L,
            true,
            false,
            sort
        );

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .contains("attachment")
            .contains("test-academy_exam_55_grades_" + expectedDate + ".csv");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.getBody().writeTo(out);

        then(examExportService).should().prepareCsvForExam(55L, 9L, true, false, sort);
    }

    @Test
    @DisplayName("exportExamResultsCsv: 학생 권한은 거부한다")
    void exportExamResultsCsv_rejectsStudentRole() {
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> controller.exportExamResultsCsv(
            55L,
            null,
            null,
            null,
            Sort.unsorted()
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("INSTRUCTOR role required");

        then(examExportService).shouldHaveNoInteractions();
    }

    @Test
    void omrIngestRejectsStudentBeforeProcessingSigningOrQueueing() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> controller.processOmrGrading(
                        55L, 9L, new org.springframework.mock.web.MockMultipartFile(
                                "image", "exam.jpg", "image/jpeg", new byte[]{1, 2, 3}), null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> controller.requestPresignedUrls(
                        55L, new OmrPresignRequest(List.of("page.png")), null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> controller.confirmBatchUpload(
                        55L, new OmrBatchConfirmRequest("batch-1", List.of("object-key")), null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));

        org.mockito.Mockito.verifyNoInteractions(resultCommandService, idempotencyService);
    }

    @Test
    void instructorCanStartEachOmrIngestRoute() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        org.mockito.BDDMockito.given(resultCommandService.confirmBatchOmrGrading(
                        55L, new OmrBatchConfirmRequest("batch-1", List.of("object-key"))))
                .willReturn(new OmrJobResponse(12L));
        org.mockito.BDDMockito.given(idempotencyService.executeOnceWithoutResponseBody(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        org.mockito.BDDMockito.given(idempotencyService.executeOnce(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());

        var single = controller.processOmrGrading(
                55L, 9L, new org.springframework.mock.web.MockMultipartFile(
                        "image", "exam.jpg", "image/jpeg", new byte[]{1, 2, 3}), null);
        var presigned = controller.requestPresignedUrls(
                55L, new OmrPresignRequest(List.of("page.png")), null);
        var batch = controller.confirmBatchUpload(
                55L, new OmrBatchConfirmRequest("batch-1", List.of("object-key")), null);

        assertThat(single.getStatusCode().value()).isEqualTo(201);
        assertThat(presigned.getStatusCode().value()).isEqualTo(200);
        assertThat(batch.getStatusCode().value()).isEqualTo(202);

        assertThat(controller.processOmrGrading(
                        55L, 9L, new org.springframework.mock.web.MockMultipartFile(
                                "image", "exam.jpg", "image/jpeg", new byte[]{1, 2, 3}), "single-key")
                .getStatusCode().value()).isEqualTo(201);
        assertThat(controller.requestPresignedUrls(
                        55L, new OmrPresignRequest(List.of("page.png")), "presign-key")
                .getStatusCode().value()).isEqualTo(200);
        assertThat(controller.confirmBatchUpload(
                        55L, new OmrBatchConfirmRequest("batch-1", List.of("object-key")), "confirm-key")
                .getStatusCode().value()).isEqualTo(202);
    }

    @Test
    @DisplayName("학생은 문항 답안 정정을 할 수 없다")
    void updateQuestionAnswer_rejectsStudentRole() {
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> controller.updateQuestionAnswer(
                17L, new UpdateQuestionAnswerRequest("2")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("INSTRUCTOR role required");

        then(resultCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("학생은 시험 결과를 삭제할 수 없다")
    void deleteResult_rejectsStudentRole() {
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> controller.deleteResult(55L, 17L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("INSTRUCTOR role required");

        then(resultCommandService).shouldHaveNoInteractions();
    }

    @Test
    void rawStudentResultsRequireStaffRole() throws Exception {
        mockMvc.perform(get("/v1/students/123/results")
                .header(TEST_ROLE_HEADER, Role.STUDENT.name())
                .header(TEST_TENANT_HEADER, "test-academy"))
            .andExpect(status().isForbidden());

        then(resultQueryService).shouldHaveNoInteractions();
    }

    @Test
    void examResultsRequireStaffRole() throws Exception {
        mockMvc.perform(get("/v1/exams/5/results")
                .header(TEST_ROLE_HEADER, Role.STUDENT.name())
                .header(TEST_TENANT_HEADER, "test-academy"))
            .andExpect(status().isForbidden());

        then(resultQueryService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("recordBatchUploadTelemetry: 강사 이상이면 클라이언트 업로드 측정값을 기록한다")
    void recordBatchUploadTelemetry_allowsInstructorAndDelegates() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        OmrUploadTelemetryRequest request = new OmrUploadTelemetryRequest(
            "batch-123",
            6,
            1_200,
            3_000,
            "4g",
            List.of(new OmrUploadTelemetryRequest.OmrUploadTelemetryFile(
                0,
                3_000,
                3_000,
                1_200,
                200,
                OmrUploadTelemetryRequest.OmrUploadOutcome.SUCCESS
            ))
        );

        controller.recordBatchUploadTelemetry(55L, request);

        then(omrUploadTelemetryService).should().record(55L, request);
    }

    @Test
    @DisplayName("exportExamResultsCsv: 쿼리 파라미터와 반복 sort가 서비스 호출로 바인딩된다")
    void exportExamResultsCsv_bindsQueryParametersAndRepeatedSort() throws Exception {
        org.mockito.BDDMockito.given(examExportService.prepareCsvForExam(
            org.mockito.ArgumentMatchers.eq(55L),
            org.mockito.ArgumentMatchers.eq(9L),
            org.mockito.ArgumentMatchers.eq(true),
            org.mockito.ArgumentMatchers.eq(false),
            org.mockito.ArgumentMatchers.any(Sort.class)
        )).willReturn(new ExamExportService.CsvExport(List.of(), List.of()));

        mockMvc.perform(get("/v1/exams/55/results/export.csv")
                .header(TEST_ROLE_HEADER, Role.INSTRUCTOR.name())
                .header(TEST_TENANT_HEADER, "test-academy")
                .param("classId", "9")
                .param("hasActiveEnrollment", "true")
                .param("isActive", "false")
                .param("sort", "rank,desc")
                .param("sort", "totalScore,asc"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"));

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        then(examExportService).should().prepareCsvForExam(
            eq(55L),
            eq(9L),
            eq(true),
            eq(false),
            sortCaptor.capture()
        );

        List<Sort.Order> orders = sortCaptor.getValue().stream().toList();
        assertThat(orders)
            .extracting(Sort.Order::getProperty)
            .containsExactly("rank", "totalScore");
        assertThat(orders)
            .extracting(Sort.Order::getDirection)
            .containsExactly(Sort.Direction.DESC, Sort.Direction.ASC);
    }

    @Test
    @DisplayName("exportResultsCsv: 기존 전역 export는 OWNER가 아니면 거부된다")
    void exportResultsCsv_requiresOwner() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/exams/results/export.csv")
                .header(TEST_ROLE_HEADER, Role.INSTRUCTOR.name())
                .header(TEST_TENANT_HEADER, "test-academy"))
            .andExpect(status().isForbidden());
    }

    private static final class TestRequestContextFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            String tenant = request.getHeader(TEST_TENANT_HEADER);
            String role = request.getHeader(TEST_ROLE_HEADER);

            if (tenant != null) {
                TenantContextHolder.setTenant(tenant);
            }
            if (role != null) {
                UserContextHolder.setUserRole(Role.valueOf(role));
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContextHolder.clear();
                UserContextHolder.clear();
            }
        }
    }
}
