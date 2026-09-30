package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.ApplyOmrSubmissionRequest;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.dto.request.ConfirmOmrSubmissionReconciliationRequest;
import com.lumie.exam.application.dto.request.PreviewOmrSubmissionReconciliationRequest;
import com.lumie.exam.application.dto.request.RegisterStudentFromOmrSubmissionRequest;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.application.dto.response.OmrSubmissionResponse;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.repository.OmrSubmissionRepository;
import com.lumie.exam.domain.vo.OmrGradingStatus;
import com.lumie.exam.domain.vo.OmrReconciliationStatus;
import com.lumie.student.api.StudentService;
import com.lumie.student.api.StudentRegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("OmrSubmissionService manual reconciliation")
class OmrSubmissionServiceTest {

    @Mock private OmrSubmissionRepository submissionRepository;
    @Mock private ExamResultRepository resultRepository;
    @Mock private OmrGradingJobRepository jobRepository;
    @Mock private ResultCommandService resultCommandService;
    @Mock private StudentService studentService;
    @Mock private StudentRegistrationService studentRegistrationService;
    @InjectMocks private OmrSubmissionService service;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenant("test-academy");
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void passFailCallbackKeepsScoreWithoutPersistingWorkerNumericGrade() {
        Exam exam = Exam.create("단어 시험", com.lumie.exam.domain.vo.ExamCategory.PASS_FAIL,
                com.lumie.exam.domain.vo.GradingType.ABSOLUTE,
                com.lumie.exam.domain.vo.GradeScale.NINE_GRADE, 1,
                Map.of("1", "1"), Map.of("1", 20), Map.of("1", "어휘"), 15);
        OmrSubmission submission = OmrSubmission.pending(
                exam, 62L, 0, "temp/omr.png", java.time.Instant.parse("2026-09-30T00:00:00Z"));
        given(submissionRepository.findByJobIdAndImageIndexForUpdate(62L, 0))
                .willReturn(Optional.of(submission));
        OmrGradingCallbackRequest callback = new OmrGradingCallbackRequest(
                62L, 81L, "test-academy", 0, 1, true, null, null, 20, 8, List.of());

        assertThat(service.completeIfPending(62L, 0, callback, "omr/submission.png")).isTrue();
        assertThat(submission.getTotalScore()).isEqualTo(20);
        assertThat(submission.getGrade()).isNull();
        assertThat(OmrSubmissionResponse.from(submission, null, false).grade()).isNull();
    }

    @Test
    void passFailMatchedSubmissionStillCreatesOfficialResultWithNullGrade() {
        Exam exam = Exam.create("단어 시험", com.lumie.exam.domain.vo.ExamCategory.PASS_FAIL,
                com.lumie.exam.domain.vo.GradingType.ABSOLUTE,
                com.lumie.exam.domain.vo.GradeScale.NINE_GRADE, 1,
                Map.of("1", "1"), Map.of("1", 20), Map.of("1", "어휘"), 15);
        org.springframework.test.util.ReflectionTestUtils.setField(exam, "id", 81L);
        OmrSubmission submission = OmrSubmission.pending(
                exam, 63L, 0, "temp/omr.png", java.time.Instant.parse("2026-09-30T00:00:00Z"));
        given(submissionRepository.findByJobIdAndImageIndexForUpdate(63L, 0))
                .willReturn(Optional.of(submission));
        given(studentService.getStudentByPhone("test-academy", "01012345678"))
                .willReturn(Optional.of(new StudentService.StudentData(
                        7L, 8L, "학생", "01012345678", true, null, null)));
        given(resultCommandService.resolveResult(eq(exam), eq(7L), any())).willAnswer(invocation -> {
            com.lumie.exam.application.port.out.OmrServicePort.OmrGradingResult grading = invocation.getArgument(2);
            assertThat(grading.grade()).isNull();
            return ExamResult.create(exam, 7L, grading.totalScore());
        });
        OmrGradingCallbackRequest callback = new OmrGradingCallbackRequest(
                63L, 81L, "test-academy", 0, 1, true, null, "01012345678", 20, 8, List.of());

        assertThat(service.completeIfPending(63L, 0, callback, "omr/submission.png")).isTrue();
        assertThat(submission.getGrade()).isNull();
        org.mockito.Mockito.verify(resultRepository).saveAndFlush(any(ExamResult.class));
    }

    @Test
    @DisplayName("pending submission은 수동 공식 성적 반영을 거부한다")
    void applyRejectsPendingSubmission() {
        OmrSubmission submission = submission(10L, OmrGradingStatus.PENDING);
        given(submissionRepository.findById(10L)).willReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.apply(1L, 10L, new ApplyOmrSubmissionRequest(1L, true)))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(studentService, resultRepository, resultCommandService);
    }

    @Test
    @DisplayName("다른 시험의 job submission 조회는 빈 목록 대신 거부한다")
    void getJobSubmissionsRejectsJobFromAnotherExam() {
        var job = mock(com.lumie.exam.domain.entity.OmrGradingJob.class);
        var jobExam = mock(Exam.class);
        given(jobExam.getId()).willReturn(2L);
        given(job.getExam()).willReturn(jobExam);
        given(jobRepository.findById(10L)).willReturn(Optional.of(job));

        assertThatThrownBy(() -> service.getJobSubmissions(1L, 10L))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(submissionRepository);
    }

    @Test
    @DisplayName("이미 다른 공식 성적에 반영된 submission은 재소유할 수 없다")
    void applyRejectsSubmissionAlreadyOwnedByAnotherResult() {
        OmrSubmission submission = submission(10L, OmrGradingStatus.SUCCEEDED);
        ExamResult owner = mock(ExamResult.class);
        given(submissionRepository.findById(10L)).willReturn(Optional.of(submission));
        given(studentService.getStudent("test-academy", 1L)).willReturn(Optional.of(
                new StudentService.StudentData(1L, 2L, "학생", "01012345678", true, null, null)));
        given(resultRepository.findByExamIdAndStudentId(1L, 1L)).willReturn(Optional.empty());
        given(resultRepository.findByCurrentSubmissionId(10L)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.apply(1L, 10L, new ApplyOmrSubmissionRequest(1L, true)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("수동 apply의 unique race는 DUPLICATE_RESULT로 변환한다")
    void applyTranslatesUniqueRaceToBusinessConflict() {
        OmrSubmission submission = officialSubmission(10L);
        ExamResult projected = mock(ExamResult.class);
        given(submissionRepository.findById(10L)).willReturn(Optional.of(submission));
        given(studentService.getStudent("test-academy", 1L)).willReturn(Optional.of(
                new StudentService.StudentData(1L, 2L, "학생", "01012345678", true, null, null)));
        given(resultRepository.findByExamIdAndStudentId(1L, 1L)).willReturn(Optional.empty());
        given(resultRepository.findByCurrentSubmissionId(10L)).willReturn(Optional.empty());
        given(resultCommandService.resolveResult(eq(submission.getExam()), eq(1L), any()))
                .willReturn(projected);
        given(resultRepository.saveAndFlush(projected)).willThrow(uniqueViolation());

        assertThatThrownBy(() -> service.apply(1L, 10L, new ApplyOmrSubmissionRequest(1L, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.lumie.exam.domain.exception.ExamErrorCode.DUPLICATE_RESULT);
    }

    @Test
    @DisplayName("수동 register-and-apply의 unique race는 DUPLICATE_RESULT로 변환한다")
    void registerStudentAndApplyTranslatesUniqueRaceToBusinessConflict() {
        OmrSubmission submission = officialSubmission(11L);
        ExamResult projected = mock(ExamResult.class);
        given(submissionRepository.findById(11L)).willReturn(Optional.of(submission));
        given(resultRepository.findByCurrentSubmissionId(11L)).willReturn(Optional.empty());
        given(studentRegistrationService.registerStudent(
                eq("test-academy"),
                any(StudentRegistrationService.RegisterStudentCommand.class)))
                .willReturn(new StudentService.StudentData(3L, 4L, "신규학생", "01012345678", true, null, null));
        given(resultCommandService.resolveResult(eq(submission.getExam()), eq(3L), any()))
                .willReturn(projected);
        given(resultRepository.saveAndFlush(projected)).willThrow(uniqueViolation());

        RegisterStudentFromOmrSubmissionRequest request = new RegisterStudentFromOmrSubmissionRequest(
                "신규학생", "", "고등학교", 2008, null, null);

        assertThatThrownBy(() -> service.registerStudentAndApply(1L, 11L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.lumie.exam.domain.exception.ExamErrorCode.DUPLICATE_RESULT);
    }

    @Test
    @DisplayName("학생 등록 backfill unique race는 DUPLICATE_RESULT로 변환한다")
    void linkSingleUnmatchedByPhoneTranslatesUniqueRaceToBusinessConflict() {
        OmrSubmission submission = officialSubmission(12L);
        ExamResult projected = mock(ExamResult.class);
        given(submissionRepository.findByEffectivePhoneNumberAndReconciliationStatus(
                "01012345678", OmrReconciliationStatus.UNMATCHED)).willReturn(List.of(submission));
        given(resultRepository.findByExamIdAndStudentId(1L, 5L)).willReturn(Optional.empty());
        given(studentService.getStudent("test-academy", 5L)).willReturn(Optional.of(
                new StudentService.StudentData(5L, 6L, "학생", "01012345678", true, null, null)));
        given(resultCommandService.resolveResult(eq(submission.getExam()), eq(5L), any()))
                .willReturn(projected);
        given(resultRepository.saveAndFlush(projected)).willThrow(uniqueViolation());

        assertThatThrownBy(() -> service.linkSingleUnmatchedByPhone("01012345678", 5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.lumie.exam.domain.exception.ExamErrorCode.DUPLICATE_RESULT);
    }

    @Test
    @DisplayName("전화번호 미리보기는 정확히 매칭된 활성 학생과 기존 성적 교체 필요 여부를 반환한다")
    void previewReconciliationFindsActiveStudentAndCurrentResult() {
        OmrSubmission submission = submission(13L, OmrGradingStatus.SUCCEEDED);
        ExamResult currentResult = mock(ExamResult.class);
        given(submissionRepository.findById(13L)).willReturn(Optional.of(submission));
        given(resultRepository.findByCurrentSubmissionId(13L)).willReturn(Optional.empty());
        given(studentService.getStudentByPhone("test-academy", "01012345678")).willReturn(Optional.of(
                new StudentService.StudentData(7L, 8L, "학생", "01012345678", true, null, null)));
        given(resultRepository.findByExamIdAndStudentId(1L, 7L)).willReturn(Optional.of(currentResult));
        Exam currentExam = submission.getExam();
        given(currentResult.getExam()).willReturn(currentExam);
        given(currentResult.getId()).willReturn(99L);
        given(currentResult.getTotalScore()).willReturn(88);
        given(currentResult.getCreatedAt()).willReturn(java.time.Instant.parse("2026-08-05T00:00:00Z"));

        var preview = service.previewReconciliation(
                1L, 13L, new PreviewOmrSubmissionReconciliationRequest("010-1234-5678"));

        assertThat(preview.student().id()).isEqualTo(7L);
        assertThat(preview.currentResult().resultId()).isEqualTo(99L);
        assertThat(preview.replaceRequired()).isTrue();
        verifyNoInteractions(resultCommandService);
    }

    @Test
    @DisplayName("공식 반영 전 자동 매칭 submission도 전화번호 미리보기로 다시 확인할 수 있다")
    void previewReconciliationAllowsUnofficialMatchedSubmission() {
        OmrSubmission submission = submission(16L, OmrGradingStatus.SUCCEEDED);
        given(submissionRepository.findById(16L)).willReturn(Optional.of(submission));
        given(resultRepository.findByCurrentSubmissionId(16L)).willReturn(Optional.empty());
        given(studentService.getStudentByPhone("test-academy", "01012345678")).willReturn(Optional.of(
                new StudentService.StudentData(7L, 8L, "학생", "01012345678", true, null, null)));
        given(resultRepository.findByExamIdAndStudentId(1L, 7L)).willReturn(Optional.empty());

        var preview = service.previewReconciliation(
                1L, 16L, new PreviewOmrSubmissionReconciliationRequest("01012345678"));

        assertThat(preview.student().id()).isEqualTo(7L);
        assertThat(preview.replaceRequired()).isFalse();
    }

    @Test
    @DisplayName("비활성 학생 번호 미리보기는 submission 상태를 바꾸지 않고 명시적 오류를 낸다")
    void previewReconciliationDoesNotMutateForInactiveStudentPhone() {
        OmrSubmission submission = submission(14L, OmrGradingStatus.SUCCEEDED);
        given(submissionRepository.findById(14L)).willReturn(Optional.of(submission));
        given(resultRepository.findByCurrentSubmissionId(14L)).willReturn(Optional.empty());
        given(studentService.getStudentByPhone("test-academy", "01012345678")).willReturn(Optional.of(
                new StudentService.StudentData(7L, 8L, "퇴원생", "01012345678", false, null, null)));

        assertThatThrownBy(() -> service.previewReconciliation(
                1L, 14L, new PreviewOmrSubmissionReconciliationRequest("01012345678")))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ExamErrorCode.INACTIVE_STUDENT_PHONE);
                    assertThat(e.getErrorCode().getCode()).isEqualTo("STUDENT_205");
                    assertThat(e.getMessage()).isEqualTo("이 전화번호는 퇴원 학생 기록에 존재합니다.");
                });

        org.mockito.Mockito.verify(submission, org.mockito.Mockito.never()).reject("INACTIVE_STUDENT_PHONE");
    }

    @Test
    @DisplayName("비활성 학생 번호 확정은 submission을 거절 상태로 남기고 공통 오류 코드를 낸다")
    void confirmReconciliationRejectsInactiveStudentPhone() {
        OmrSubmission submission = submission(18L, OmrGradingStatus.SUCCEEDED);
        given(submissionRepository.findById(18L)).willReturn(Optional.of(submission));
        given(resultRepository.findByCurrentSubmissionId(18L)).willReturn(Optional.empty());
        given(studentService.getStudentByPhone("test-academy", "01012345678")).willReturn(Optional.of(
                new StudentService.StudentData(7L, 8L, "퇴원생", "01012345678", false, null, null)));

        assertThatThrownBy(() -> service.confirmReconciliation(
                1L, 18L, new ConfirmOmrSubmissionReconciliationRequest("01012345678", 7L, null, false)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ExamErrorCode.INACTIVE_STUDENT_PHONE);
                    assertThat(e.getErrorCode().getCode()).isEqualTo("STUDENT_205");
                    assertThat(e.getMessage()).isEqualTo("이 전화번호는 퇴원 학생 기록에 존재합니다.");
                });

        org.mockito.Mockito.verify(submission).reject("INACTIVE_STUDENT_PHONE");
    }

    @Test
    @DisplayName("전화번호 확정은 미리보기의 학생 ID를 검증하고 서버에서 다시 찾은 학생에게만 반영한다")
    void confirmReconciliationResolvesStudentByPhoneOnServer() {
        OmrSubmission submission = officialSubmission(15L);
        ExamResult projected = mock(ExamResult.class);
        given(submissionRepository.findById(15L)).willReturn(Optional.of(submission));
        given(resultRepository.findByCurrentSubmissionId(15L)).willReturn(Optional.empty());
        given(studentService.getStudentByPhone("test-academy", "01012345678")).willReturn(Optional.of(
                new StudentService.StudentData(7L, 8L, "학생", "01012345678", true, null, null)));
        given(resultRepository.findByExamIdAndStudentId(1L, 7L)).willReturn(Optional.empty());
        given(resultCommandService.resolveResult(eq(submission.getExam()), eq(7L), any())).willReturn(projected);

        service.confirmReconciliation(1L, 15L,
                new ConfirmOmrSubmissionReconciliationRequest("01012345678", 7L, null, false));

        org.mockito.Mockito.verify(submission).correctPhone("01012345678");
        org.mockito.Mockito.verify(submission).markMatched(7L);
        verifyNoInteractions(studentRegistrationService);
        org.mockito.Mockito.verify(studentService, org.mockito.Mockito.never()).getStudent(any(), any());
    }

    @Test
    @DisplayName("전화번호 확정의 공식 성적 저장 충돌은 submission 상태를 변경하지 않는다")
    void confirmReconciliationDoesNotMutateSubmissionWhenProjectionConflicts() {
        OmrSubmission submission = officialSubmission(17L);
        ExamResult projected = mock(ExamResult.class);
        given(submissionRepository.findById(17L)).willReturn(Optional.of(submission));
        given(resultRepository.findByCurrentSubmissionId(17L)).willReturn(Optional.empty());
        given(studentService.getStudentByPhone("test-academy", "01012345678")).willReturn(Optional.of(
                new StudentService.StudentData(7L, 8L, "학생", "01012345678", true, null, null)));
        given(resultRepository.findByExamIdAndStudentId(1L, 7L)).willReturn(Optional.empty());
        given(resultCommandService.resolveResult(eq(submission.getExam()), eq(7L), any())).willReturn(projected);
        given(resultRepository.saveAndFlush(projected)).willThrow(uniqueViolation());

        assertThatThrownBy(() -> service.confirmReconciliation(1L, 17L,
                new ConfirmOmrSubmissionReconciliationRequest("01012345678", 7L, null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.lumie.exam.domain.exception.ExamErrorCode.DUPLICATE_RESULT);

        org.mockito.Mockito.verify(submission, org.mockito.Mockito.never()).correctPhone(any());
        org.mockito.Mockito.verify(submission, org.mockito.Mockito.never()).markMatched(any());
    }

    private OmrSubmission submission(Long id, OmrGradingStatus gradingStatus) {
        OmrSubmission submission = mock(OmrSubmission.class);
        Exam exam = mock(Exam.class);
        given(exam.getId()).willReturn(1L);
        lenient().when(submission.getId()).thenReturn(id);
        given(submission.getExam()).willReturn(exam);
        lenient().when(submission.getGradingStatus()).thenReturn(gradingStatus);
        return submission;
    }

    private OmrSubmission officialSubmission(Long id) {
        OmrSubmission submission = submission(id, OmrGradingStatus.SUCCEEDED);
        lenient().when(submission.getJobId()).thenReturn(100L);
        lenient().when(submission.getOmrImageKey()).thenReturn("omr/submission.png");
        lenient().when(submission.getOcrPhoneNumber()).thenReturn("01012345678");
        lenient().when(submission.getEffectivePhoneNumber()).thenReturn("01012345678");
        lenient().when(submission.getTotalScore()).thenReturn(100);
        lenient().when(submission.getGrade()).thenReturn(1);
        lenient().when(submission.getQuestions()).thenReturn(List.of());
        return submission;
    }

    private static DataIntegrityViolationException uniqueViolation() {
        return new DataIntegrityViolationException("duplicate key", new SQLException("duplicate key", "23505"));
    }
}
