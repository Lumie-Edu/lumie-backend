package com.lumie.exam.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.dto.request.ExamAnswerRegradeConfirmRequest;
import com.lumie.exam.application.dto.response.ExamAnswerRegradeRevisionResponse;
import com.lumie.exam.application.service.ExamRegradeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ExamAnswerRegradeControllerTest {

    private final ExamRegradeService regradeService = mock(ExamRegradeService.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserId(10L);
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.setTenantId(101L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ExamAnswerRegradeController(regradeService, idempotencyService)).build();
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<ResponseEntity<ExamAnswerRegradeRevisionResponse>> action =
                    (Supplier<ResponseEntity<ExamAnswerRegradeRevisionResponse>>) invocation.getArgument(3);
            return action.get();
        }).when(idempotencyService).executeOnceAtomically(anyString(), anyString(), any(), any());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void confirmRequiresStaffAndRoutesThroughIdempotencyWithAuthenticatedActor() throws Exception {
        ExamAnswerRegradeRevisionResponse response = new ExamAnswerRegradeRevisionResponse(
                90L, 5L, 13L, null, 10L,
                Map.of("44", new ExamAnswerRegradeRevisionResponse.ChangedAnswer("2", "4")), Map.of(),
                80, 0, 117, false);
        when(regradeService.confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L)))
                .thenReturn(response);

        var result = mockMvc.perform(post("/v1/exams/5/answer-key-revisions")
                        .header("Idempotency-Key", "exam-regrade-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Regrade","answerChanges":{"44":"4"},"questionScores":{"44":2},"questionTypes":{"44":"CHOICE"}}
                                """))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("\"revisionId\":90");
        verify(idempotencyService).executeOnceAtomically(
                eq("exam-regrade-1"), contains("answer-key-revisions"), any(), any());
        verify(regradeService).confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L));
    }

    @Test
    void studentCannotConfirmAnswerCorrection() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> mockMvc.perform(post("/v1/exams/5/answer-key-revisions")
                        .header("Idempotency-Key", "exam-regrade-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Regrade","answerChanges":{"44":"4"},"questionScores":{"44":2},"questionTypes":{"44":"CHOICE"}}
                                """))
                .andReturn())
                .hasRootCauseInstanceOf(BusinessException.class);
        verifyNoInteractions(regradeService, idempotencyService);
    }
}
