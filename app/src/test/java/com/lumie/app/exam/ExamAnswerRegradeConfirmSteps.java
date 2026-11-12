package com.lumie.app.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.common.auth.Role;
import com.lumie.exam.application.dto.request.ExamAnswerRegradeConfirmRequest;
import com.lumie.exam.application.dto.response.ExamAnswerRegradeRevisionResponse;
import com.lumie.exam.application.dto.response.ExamAnswerKeyRevisionResponse;
import com.lumie.exam.application.service.ExamRegradeService;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.Before;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class ExamAnswerRegradeConfirmSteps {

    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private MockMvc mockMvc;
    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private AuthQueryService authQueryService;
    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private SessionConfig sessionConfig;
    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private ExamRegradeService examRegradeService;
    // lint: allow B-no-field-injection -- Cucumber Spring injects scenario glue fields.
    @Autowired private JdbcTemplate jdbcTemplate;

    private Cookie sessionCookie;
    private String sessionId;
    private MvcResult response;
    private MvcResult firstResponse;

    private static final String CORRECTION_BODY = """
            {"name":"Regrade","answerChanges":{"44":"4"},
             "questionScores":{"44":2},"questionTypes":{"44":"CHOICE"}}
            """;

    @Before("@ATDD-US1-002 or @ATDD-US1-014")
    public void createIdempotencyFixture() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.idempotency_keys (
                    id BIGSERIAL PRIMARY KEY,
                    tenant_slug VARCHAR(80) NOT NULL,
                    endpoint_path VARCHAR(255) NOT NULL,
                    idempotency_key VARCHAR(255) NOT NULL,
                    request_fingerprint VARCHAR(64) NOT NULL,
                    response_status INT NOT NULL,
                    response_body TEXT,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                    CONSTRAINT uq_idempotency_keys UNIQUE (tenant_slug, endpoint_path, idempotency_key)
                )
                """);
    }

    @Given("an authorized instructor is editing exam {long}")
    public void anInstructorIsEditingExam(long examId) {
        createIdempotencyFixture();
        clearInvocations(authQueryService, examRegradeService);
        sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        given(authQueryService.findActiveSession(sessionId)).willReturn(Optional.of(new AuthSession(
                sessionId, 10L, "Instructor", "test-tenant", 20L, Role.INSTRUCTOR,
                "test", DeviceCategory.UNKNOWN, now, now.plusSeconds(3600))));
        sessionCookie = new Cookie(sessionConfig.getCookieName(), sessionId);
        given(examRegradeService.confirm(
                eq(examId), any(ExamAnswerRegradeConfirmRequest.class), eq(10L)))
                .willReturn(new ExamAnswerRegradeRevisionResponse(
                        90L, examId, 13L, Instant.parse("2026-11-10T00:00:00Z"), 10L,
                        Map.of("44", new ExamAnswerRegradeRevisionResponse.ChangedAnswer("2", "4")),
                        Map.of("name", new ExamAnswerRegradeRevisionResponse.ChangedAnswer("Regrade", "Renamed")),
                        80, 0, 117, false));
    }

    @When("the instructor confirms the correction with an idempotency key")
    public void theInstructorConfirmsTheCorrection() throws Exception {
        response = mockMvc.perform(post("/v1/exams/5/answer-key-revisions")
                        .cookie(sessionCookie)
                        .header("Host", "localhost")
                        .header("Origin", "http://localhost")
                        .header("X-Tenant-Slug", "test-tenant")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Regrade","answerChanges":{"44":"4"},"questionScores":{"44":2},"questionTypes":{"44":"CHOICE"}}
                                """))
                .andReturn();
    }

    @Then("the official regrade receipt contains the affected record counts")
    public void theOfficialRegradeReceiptContainsAffectedCounts() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString())
                .contains("\"revisionId\":90", "\"regradedResultCount\":80", "\"updatedSubmissionCount\":117");
        verify(examRegradeService).confirm(
                eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L));
    }

    @When("the instructor confirms a name, answer, score, and type correction")
    public void theInstructorConfirmsExamFields() throws Exception {
        response = mockMvc.perform(post("/v1/exams/5/answer-key-revisions")
                        .cookie(sessionCookie)
                        .header("Host", "localhost")
                        .header("Origin", "http://localhost")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed","answerChanges":{"44":"4"},
                                 "questionScores":{"44":3},"questionTypes":{"44":"READING"}}
                                """))
                .andReturn();
    }

    @Then("the correction request and receipt include the changed exam fields")
    public void theCorrectionRequestAndReceiptIncludeFields() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString()).contains("changedFields", "Renamed");
        verify(examRegradeService).confirm(eq(5L), argThat(request ->
                        request.name().equals("Renamed")
                                && request.answerChanges().equals(Map.of("44", "4"))
                                && request.questionScores().equals(Map.of("44", 3))
                                && request.questionTypes().equals(Map.of("44", "READING"))), eq(10L));
    }

    @When("the instructor reads correction history")
    public void theInstructorReadsCorrectionHistory() throws Exception {
        var revision = new ExamAnswerKeyRevisionResponse(90L, 10L,
                Instant.parse("2026-11-10T00:00:00Z"),
                Map.of("44", new ExamAnswerKeyRevisionResponse.AnswerKeyChange("2", "4")),
                Map.of("name", new ExamAnswerKeyRevisionResponse.AnswerKeyChange("Old", "Renamed")),
                80, 0, 117);
        given(examRegradeService.history(eq(5L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(revision)));
        response = mockMvc.perform(get("/v1/exams/5/answer-key-revisions")
                        .cookie(sessionCookie).header("Host", "localhost"))
                .andReturn();
    }

    @Then("the history shows the recorded correction details")
    public void theHistoryShowsRecordedDetails() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString())
                .contains("\"actorUserId\":10", "2026-11-10", "changedAnswers", "changedFields", "Renamed");
    }

    @Given("another correction conflicts with this one")
    public void anotherCorrectionConflicts() {
        given(examRegradeService.confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L)))
                .willThrow(new OptimisticLockingFailureException("concurrent update"));
    }

    @Then("the correction reports a conflict")
    public void theCorrectionReportsConflict() {
        assertThat(response.getResponse().getStatus()).isEqualTo(409);
    }

    @Given("saved question data is incomplete")
    public void savedQuestionDataIsIncomplete() {
        given(examRegradeService.confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L)))
                .willThrow(new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA));
    }

    @Then("the correction reports invalid answer data")
    public void theCorrectionReportsInvalidAnswerData() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(400);
        assertThat(response.getResponse().getContentAsString()).contains("EXAM_003");
    }

    @Given("the exam includes two manually overridden results")
    public void theExamIncludesTwoManualOverrides() {
        given(examRegradeService.confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L)))
                .willReturn(receipt(78, 2, 117));
    }

    @Then("the receipt reports two skipped manual overrides")
    public void theReceiptReportsTwoSkippedOverrides() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString())
                .contains("\"skippedManualOverrideCount\":2");
    }

    @Given("the submitted exam information is unchanged")
    public void theSubmittedExamInformationIsUnchanged() {
        given(examRegradeService.confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L)))
                .willReturn(ExamAnswerRegradeRevisionResponse.noOp(5L, 12L, Map.of()));
    }

    @Then("the receipt reports no change and no revision")
    public void theReceiptReportsNoChange() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString())
                .contains("\"noOp\":true", "\"revisionId\":null");
    }

    @When("the instructor confirms a correction for an exam without results")
    public void theInstructorConfirmsWithoutResults() throws Exception {
        given(examRegradeService.confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L)))
                .willReturn(receipt(0, 0, 0));
        response = performConfirm(CORRECTION_BODY, UUID.randomUUID().toString());
    }

    @Then("the receipt records zero regraded results and submissions")
    public void theReceiptRecordsZeroResults() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString())
                .contains("\"regradedResultCount\":0", "\"updatedSubmissionCount\":0");
    }

    @When("the instructor confirms two different corrections with one idempotency key")
    public void theInstructorReusesOneKey() throws Exception {
        String key = UUID.randomUUID().toString();
        firstResponse = performConfirm(CORRECTION_BODY, key);
        response = performConfirm(CORRECTION_BODY.replace("Regrade", "Renamed"), key);
    }

    @Then("only the first correction is applied and the second conflicts")
    public void onlyTheFirstCorrectionIsApplied() {
        assertThat(firstResponse.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getStatus()).isEqualTo(409);
        verify(examRegradeService, times(1)).confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L));
    }

    @Given("a student is editing exam 5")
    public void aStudentIsEditingExam() {
        anInstructorIsEditingExam(5L);
        Instant now = Instant.now();
        given(authQueryService.findActiveSession(sessionId)).willReturn(Optional.of(new AuthSession(
                sessionId, 10L, "Student", "test-tenant", 20L, Role.STUDENT,
                "test", DeviceCategory.UNKNOWN, now, now.plusSeconds(3600))));
    }

    @When("the student confirms the correction")
    public void theStudentConfirmsTheCorrection() throws Exception {
        response = performConfirm(CORRECTION_BODY, UUID.randomUUID().toString());
    }

    @Then("the correction is denied before regrade logic runs")
    public void theCorrectionIsDeniedBeforeRegradeLogic() {
        assertThat(response.getResponse().getStatus()).isEqualTo(403);
        verifyNoInteractions(examRegradeService);
    }

    @Given("a completed unmatched OMR submission is affected")
    public void aCompletedUnmatchedOmrIsAffected() {
        given(examRegradeService.confirm(eq(5L), any(ExamAnswerRegradeConfirmRequest.class), eq(10L)))
                .willReturn(receipt(1, 0, 1));
    }

    @Then("the receipt counts the updated OMR submission")
    public void theReceiptCountsTheOmr() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString())
                .contains("\"updatedSubmissionCount\":1");
    }

    private MvcResult performConfirm(String body, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/v1/exams/5/answer-key-revisions")
                        .cookie(sessionCookie)
                        .header("Host", "localhost")
                        .header("Origin", "http://localhost")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private static ExamAnswerRegradeRevisionResponse receipt(int regraded, int skipped, int submissions) {
        return new ExamAnswerRegradeRevisionResponse(
                90L, 5L, 13L, Instant.parse("2026-11-10T00:00:00Z"), 10L,
                Map.of("44", new ExamAnswerRegradeRevisionResponse.ChangedAnswer("2", "4")),
                Map.of(), regraded, skipped, submissions, false);
    }
}
