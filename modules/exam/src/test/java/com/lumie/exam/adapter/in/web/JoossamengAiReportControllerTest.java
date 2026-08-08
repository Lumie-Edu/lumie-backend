package com.lumie.exam.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.adapter.out.external.JoossamengAiReportClient;
import com.lumie.exam.application.dto.request.UpdateAiCommentaryRequest;
import com.lumie.exam.application.dto.response.AiCommentaryResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JoossamengAiReportControllerTest {

    private static final String TEST_TENANT_HEADER = "X-Test-Tenant";
    private static final String TEST_ROLE_HEADER = "X-Test-Role";

    @Mock
    private JoossamengAiReportClient client;

    @Mock
    private IdempotencyService idempotencyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new JoossamengAiReportController(client, idempotencyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TestRequestContextFilter())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    void getAiCommentary_rejectsNonJoossamengTenant() throws Exception {
        mockMvc.perform(get("/v1/exams/55/ai-commentary")
                        .header(TEST_TENANT_HEADER, "other-tenant"))
                .andExpect(status().isForbidden());

        then(client).shouldHaveNoInteractions();
    }

    @Test
    void getAiCommentary_returnsEmptyPayloadWhenWorkerHasNoCommentary() throws Exception {
        given(client.getAiCommentary(55L, "joossameng"))
                .willReturn(AiCommentaryResponse.empty());

        mockMvc.perform(get("/v1/exams/55/ai-commentary")
                        .header(TEST_TENANT_HEADER, "joossameng")
                        .header(TEST_ROLE_HEADER, Role.INSTRUCTOR.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(nullValue()))
                .andExpect(jsonPath("$.isEdited").value(false))
                .andExpect(jsonPath("$.generatedAt").value(nullValue()))
                .andExpect(jsonPath("$.editedAt").value(nullValue()))
                .andExpect(jsonPath("$.version").value(nullValue()));
    }

    @Test
    void getAiCommentary_rejectsStudentRole() throws Exception {
        mockMvc.perform(get("/v1/exams/55/ai-commentary")
                        .header(TEST_TENANT_HEADER, "joossameng")
                        .header(TEST_ROLE_HEADER, Role.STUDENT.name()))
                .andExpect(status().isForbidden());

        then(client).shouldHaveNoInteractions();
    }

    @Test
    void generateAiCommentary_returnsWorkerPayload() throws Exception {
        Instant generatedAt = Instant.parse("2026-08-08T02:30:00Z");
        given(client.generateAiCommentary(55L, "joossameng"))
                .willReturn(new AiCommentaryResponse(
                        "Generated commentary",
                        false,
                        generatedAt,
                        null,
                        1L
                ));

        mockMvc.perform(post("/v1/exams/55/ai-commentary/generate")
                        .header(TEST_TENANT_HEADER, "joossameng")
                        .header(TEST_ROLE_HEADER, Role.INSTRUCTOR.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Generated commentary"))
                .andExpect(jsonPath("$.isEdited").value(false))
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void generateAiCommentary_rejectsStudentRole() throws Exception {
        mockMvc.perform(post("/v1/exams/55/ai-commentary/generate")
                        .header(TEST_TENANT_HEADER, "joossameng")
                        .header(TEST_ROLE_HEADER, Role.STUDENT.name()))
                .andExpect(status().isForbidden());

        then(client).shouldHaveNoInteractions();
    }

    @Test
    void updateAiCommentary_rejectsStudentRole() throws Exception {
        mockMvc.perform(put("/v1/exams/55/ai-commentary")
                        .header(TEST_TENANT_HEADER, "joossameng")
                        .header(TEST_ROLE_HEADER, Role.STUDENT.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Edited commentary\",\"expectedVersion\":1}"))
                .andExpect(status().isForbidden());

        then(client).shouldHaveNoInteractions();
    }

    @Test
    void updateAiCommentary_allowsInstructorRole() throws Exception {
        Instant generatedAt = Instant.parse("2026-08-08T02:30:00Z");
        Instant editedAt = Instant.parse("2026-08-08T02:45:00Z");
        given(client.updateAiCommentary(
                org.mockito.ArgumentMatchers.eq(55L),
                org.mockito.ArgumentMatchers.any(UpdateAiCommentaryRequest.class),
                org.mockito.ArgumentMatchers.eq("joossameng")
        )).willReturn(new AiCommentaryResponse(
                "Edited commentary",
                true,
                generatedAt,
                editedAt,
                2L
        ));

        mockMvc.perform(put("/v1/exams/55/ai-commentary")
                        .header(TEST_TENANT_HEADER, "joossameng")
                        .header(TEST_ROLE_HEADER, Role.INSTRUCTOR.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Edited commentary\",\"expectedVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Edited commentary"))
                .andExpect(jsonPath("$.isEdited").value(true))
                .andExpect(jsonPath("$.editedAt").exists())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void updateAiCommentary_returnsConflictWhenWorkerReportsOptimisticLockConflict() throws Exception {
        given(client.updateAiCommentary(
                org.mockito.ArgumentMatchers.eq(55L),
                org.mockito.ArgumentMatchers.any(UpdateAiCommentaryRequest.class),
                org.mockito.ArgumentMatchers.eq("joossameng")
        )).willThrow(new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT));

        mockMvc.perform(put("/v1/exams/55/ai-commentary")
                        .header(TEST_TENANT_HEADER, "joossameng")
                        .header(TEST_ROLE_HEADER, Role.INSTRUCTOR.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Edited commentary\",\"expectedVersion\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMON_011"));
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
                UserContextHolder.setUserRole(role);
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
