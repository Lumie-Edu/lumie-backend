package com.lumie.app.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.RequestContextFilter;
import com.lumie.exam.application.dto.request.ExamAnswerRegradePreviewRequest;
import com.lumie.exam.application.dto.response.ExamAnswerRegradePreviewResponse;
import com.lumie.exam.application.dto.response.ExamAnswerRegradePreviewResponse.AnswerChange;
import com.lumie.exam.application.service.ExamRegradeService;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class ExamAnswerRegradePreviewSteps {

    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private MockMvc mockMvc;
    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private AuthQueryService authQueryService;
    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private SessionConfig sessionConfig;
    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private ExamRegradeService examRegradeService;

    private Cookie sessionCookie;
    private String sessionId;
    private MvcResult response;
    private List<MvcResult> crossTenantResponses;

    @Given("an authorized instructor is reviewing an exam with existing results")
    public void anAuthorizedInstructorIsReviewingAnExamWithExistingResults() {
        clearInvocations(authQueryService, examRegradeService);
        sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        given(authQueryService.findActiveSession(sessionId)).willReturn(Optional.of(new AuthSession(
                sessionId, 10L, "Instructor", "test-tenant", 20L, Role.INSTRUCTOR,
                "test", DeviceCategory.UNKNOWN, now, now.plusSeconds(3600))));
        sessionCookie = new Cookie(sessionConfig.getCookieName(), sessionId);
        given(examRegradeService.preview(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.any(ExamAnswerRegradePreviewRequest.class)))
                .willReturn(new ExamAnswerRegradePreviewResponse(
                        5L, 12L, "preview-1", false,
                        Map.of("44", new AnswerChange("2", "4")),
                        1, 0, 1, Map.of(2, 1L), Map.of("2->1", 1L)));
    }

    @Given("an authorized instructor has a pending OMR job for this exam")
    public void anAuthorizedInstructorHasAPendingOmrJob() {
        anAuthorizedInstructorIsReviewingAnExamWithExistingResults();
        given(examRegradeService.preview(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.any(ExamAnswerRegradePreviewRequest.class)))
                .willThrow(new BusinessException(ExamErrorCode.EXAM_HAS_ACTIVE_OMR_JOB));
    }

    @When("the instructor previews changing question {int} to answer {string}")
    public void theInstructorPreviewsChangingQuestionToAnswer(int questionNumber, String answer) throws Exception {
        String body = """
                {"answerChanges":{"%d":"%s"}}
                """.formatted(questionNumber, answer);
        response = mockMvc.perform(post("/v1/exams/5/answer-key-revisions/preview")
                        .cookie(sessionCookie)
                        .header("Host", "localhost")
                        .header("Origin", "http://localhost")
                        .header("X-Tenant-Slug", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Then("the preview is returned without changing saved exam data")
    public void thePreviewIsReturnedWithoutChangingSavedExamData() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString()).contains(
                "regradedResultCount", "updatedSubmissionCount", "preview-1");
    }

    @And("the preview request uses the authenticated instructor session")
    public void thePreviewRequestUsesTheAuthenticatedInstructorSession() {
        verify(authQueryService).findActiveSession(sessionId);
        verify(examRegradeService).preview(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.any(ExamAnswerRegradePreviewRequest.class));
    }

    @Then("the correction is blocked until the OMR job finishes")
    public void theCorrectionIsBlockedUntilOmrFinishes() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(409);
        assertThat(response.getResponse().getContentAsString()).contains("EXAM_203");
    }

    @When("the instructor requests correction routes under another Tenant host binding")
    public void theInstructorRequestsCorrectionRoutesUnderAnotherTenant() throws Exception {
        MvcResult previewResponse = mockMvc.perform(post("/v1/exams/5/answer-key-revisions/preview")
                        .cookie(sessionCookie)
                        .header("Host", "localhost")
                        .header("Origin", "http://localhost")
                        .requestAttr(RequestContextFilter.HOST_TENANT_SLUG_ATTRIBUTE, "other-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answerChanges":{"44":"4"}}
                                """))
                .andReturn();
        MvcResult confirmResponse = mockMvc.perform(post("/v1/exams/5/answer-key-revisions")
                        .cookie(sessionCookie)
                        .header("Host", "localhost")
                        .header("Origin", "http://localhost")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .requestAttr(RequestContextFilter.HOST_TENANT_SLUG_ATTRIBUTE, "other-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Regrade","answerChanges":{"44":"4"},
                                 "questionScores":{"44":2},"questionTypes":{"44":"CHOICE"}}
                                """))
                .andReturn();
        MvcResult historyResponse = mockMvc.perform(get("/v1/exams/5/answer-key-revisions")
                        .cookie(sessionCookie)
                        .header("Host", "localhost")
                        .requestAttr(RequestContextFilter.HOST_TENANT_SLUG_ATTRIBUTE, "other-tenant"))
                .andReturn();
        crossTenantResponses = List.of(previewResponse, confirmResponse, historyResponse);
    }

    @Then("the cross-Tenant request is denied before regrade logic runs")
    public void crossTenantRequestIsDeniedBeforeRegradeLogic() {
        assertThat(crossTenantResponses).allSatisfy(result ->
                assertThat(result.getResponse().getStatus()).isEqualTo(403));
        verifyNoInteractions(examRegradeService);
    }
}
