package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.test.ExamFixtures;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.billing.api.BillingService;
import com.lumie.student.api.StudentService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.quota.MetricType;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResultCommandService 단위 테스트")
class ResultCommandServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamResultRepository resultRepository;
    @Mock
    private OmrGradingJobRepository jobRepository;
    @Mock
    private GradeCalculator gradeCalculator;
    @Mock
    private StudentService studentService;
    @Mock
    private BillingService billingService;
    @Mock
    private OmrServicePort omrService;
    @Mock
    private OmrStoragePort storagePort;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ResultCommandService resultCommandService;

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.setTenant("test-academy");
        // The two-phase processOmrGrading wraps DB phases in TransactionTemplate.execute().
        // Run the callback inline so unit tests don't need a real PlatformTransactionManager.
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    private Exam buildExam() {
        return ExamFixtures.aGradedExam();
    }

    private OmrServicePort.OmrGradingResult buildOmrResult(int totalScore) {
        List<OmrServicePort.OmrQuestionResult> questions = List.of(
                new OmrServicePort.OmrQuestionResult(1, "1", "1", 20, 20, "SHORT"),
                new OmrServicePort.OmrQuestionResult(2, "2", "2", 20, 20, "SHORT"),
                new OmrServicePort.OmrQuestionResult(3, "3", "3", 20, 20, "SHORT"),
                new OmrServicePort.OmrQuestionResult(4, "4", "4", 20, 20, "SHORT"),
                new OmrServicePort.OmrQuestionResult(5, "5", "5", 20, 20, "SHORT")
        );
        return new OmrServicePort.OmrGradingResult(totalScore, 1, "01012345678", questions);
    }

    private StudentService.StudentData aStudent(Long studentId) {
        return new StudentService.StudentData(
                studentId, 1000L, "홍길동", "01012345678", true, null, null);
    }

    @Test
    @DisplayName("신규 학생이면 새 결과를 생성하여 저장한다")
    void processOmrGrading_newStudent_createsNewResult() {
        // given
        Long examId = 1L;
        Long studentId = 100L;
        Exam exam = buildExam();
        ExamResult savedResult = ExamResult.create(exam, studentId, 100);

        given(examRepository.findById(examId)).willReturn(Optional.of(exam));
        given(studentService.validateStudent(any(), eq(studentId))).willReturn(new StudentService.ValidateStudentResult(true, "ok", studentId));
        given(studentService.getStudent(any(), eq(studentId))).willReturn(Optional.of(aStudent(studentId)));
        given(billingService.checkQuota(any(), eq(MetricType.OMR_MONTHLY))).willReturn(new BillingService.QuotaResult(true, 0L, 1000L, MetricType.OMR_MONTHLY, "ok"));
        given(omrService.gradeOmrImage(any(), any(), any(), any(), any())).willReturn(buildOmrResult(100));
        given(resultRepository.findByExamIdAndStudentId(examId, studentId)).willReturn(Optional.empty());
        given(resultRepository.save(any(ExamResult.class))).willReturn(savedResult);

        // when
        resultCommandService.processOmrGrading(examId, studentId, new byte[]{1, 2, 3});

        // then
        verify(resultRepository).findByExamIdAndStudentId(examId, studentId);
        ArgumentCaptor<ExamResult> captor = ArgumentCaptor.forClass(ExamResult.class);
        verify(resultRepository).save(captor.capture());
        // 가입 학생의 결과에는 등록된 전화번호가 저장되어야 한다.
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("기존 결과가 있으면 기존 엔티티를 업데이트하여 저장한다")
    void processOmrGrading_existingResult_updatesInPlace() {
        // given
        Long examId = 1L;
        Long studentId = 100L;
        Exam exam = buildExam();
        ExamResult existingResult = ExamResult.create(exam, studentId, 60);
        for (int i = 1; i <= 5; i++) {
            existingResult.addQuestionResult(QuestionResult.create(i, "0", String.valueOf(i), 20));
        }

        given(examRepository.findById(examId)).willReturn(Optional.of(exam));
        given(studentService.validateStudent(any(), eq(studentId))).willReturn(new StudentService.ValidateStudentResult(true, "ok", studentId));
        given(studentService.getStudent(any(), eq(studentId))).willReturn(Optional.of(aStudent(studentId)));
        given(billingService.checkQuota(any(), eq(MetricType.OMR_MONTHLY))).willReturn(new BillingService.QuotaResult(true, 0L, 1000L, MetricType.OMR_MONTHLY, "ok"));
        given(omrService.gradeOmrImage(any(), any(), any(), any(), any())).willReturn(buildOmrResult(100));
        given(resultRepository.findByExamIdAndStudentId(examId, studentId)).willReturn(Optional.of(existingResult));
        given(resultRepository.save(any(ExamResult.class))).willReturn(existingResult);

        // when
        resultCommandService.processOmrGrading(examId, studentId, new byte[]{1, 2, 3});

        // then
        verify(resultRepository).save(existingResult);
    }

    @Test
    @DisplayName("시험이 존재하지 않으면 ExamException을 던진다")
    void processOmrGrading_examNotFound_throwsExamException() {
        given(examRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> resultCommandService.processOmrGrading(1L, 100L, new byte[]{1}))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.EXAM_NOT_FOUND);
    }

    @Test
    @DisplayName("학생이 존재하지 않으면 ExamException을 던진다")
    void processOmrGrading_studentNotFound_throwsExamException() {
        given(examRepository.findById(1L)).willReturn(Optional.of(buildExam()));
        given(studentService.validateStudent(any(), eq(100L))).willReturn(new StudentService.ValidateStudentResult(false, "not found", 100L));

        assertThatThrownBy(() -> resultCommandService.processOmrGrading(1L, 100L, new byte[]{1}))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.STUDENT_NOT_FOUND);
    }

    @Test
    @DisplayName("OMR 쿼터가 초과되면 ExamException을 던진다")
    void processOmrGrading_quotaExceeded_throwsExamException() {
        given(examRepository.findById(1L)).willReturn(Optional.of(buildExam()));
        given(studentService.validateStudent(any(), eq(100L))).willReturn(new StudentService.ValidateStudentResult(true, "ok", 100L));
        given(studentService.getStudent(any(), eq(100L))).willReturn(Optional.of(aStudent(100L)));
        given(billingService.checkQuota(any(), eq(MetricType.OMR_MONTHLY))).willReturn(new BillingService.QuotaResult(false, 1000L, 1000L, MetricType.OMR_MONTHLY, "exceeded"));

        assertThatThrownBy(() -> resultCommandService.processOmrGrading(1L, 100L, new byte[]{1}))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.OMR_QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("OMR 채점 서비스 실패 시 ExamException을 던진다")
    void processOmrGrading_omrServiceFails_throwsExamException() {
        given(examRepository.findById(1L)).willReturn(Optional.of(buildExam()));
        given(studentService.validateStudent(any(), eq(100L))).willReturn(new StudentService.ValidateStudentResult(true, "ok", 100L));
        given(studentService.getStudent(any(), eq(100L))).willReturn(Optional.of(aStudent(100L)));
        given(billingService.checkQuota(any(), eq(MetricType.OMR_MONTHLY))).willReturn(new BillingService.QuotaResult(true, 0L, 1000L, MetricType.OMR_MONTHLY, "ok"));
        given(omrService.gradeOmrImage(any(), any(), any(), any(), any()))
                .willThrow(new RuntimeException("OMR service unavailable"));

        assertThatThrownBy(() -> resultCommandService.processOmrGrading(1L, 100L, new byte[]{1}))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.OMR_GRADING_FAILED);
    }

    @Test
    @DisplayName("OMR 채점 ���공 후 billing quota가 소비된다")
    void processOmrGrading_success_consumesBillingQuota() {
        Long examId = 1L;
        Long studentId = 100L;
        Exam exam = buildExam();
        ExamResult savedResult = ExamResult.create(exam, studentId, 100);

        given(examRepository.findById(examId)).willReturn(Optional.of(exam));
        given(studentService.validateStudent(any(), eq(studentId))).willReturn(new StudentService.ValidateStudentResult(true, "ok", studentId));
        given(studentService.getStudent(any(), eq(studentId))).willReturn(Optional.of(aStudent(studentId)));
        given(billingService.checkQuota(any(), eq(MetricType.OMR_MONTHLY))).willReturn(new BillingService.QuotaResult(true, 0L, 1000L, MetricType.OMR_MONTHLY, "ok"));
        given(omrService.gradeOmrImage(any(), any(), any(), any(), any())).willReturn(buildOmrResult(100));
        given(resultRepository.findByExamIdAndStudentId(examId, studentId)).willReturn(Optional.empty());
        given(resultRepository.save(any(ExamResult.class))).willReturn(savedResult);

        resultCommandService.processOmrGrading(examId, studentId, new byte[]{1});

        // consumeOmrQuota was a no-op adapter method; quota tracking is elsewhere.
        verify(billingService).checkQuota(any(), eq(MetricType.OMR_MONTHLY));
    }

    @Test
    @DisplayName("미등록 학생은 phoneNumber로 새 결과를 생성한다")
    void resolveResultForUnregistered_newPhone_createsResult() {
        Exam exam = buildExam();
        String phoneNumber = "01099999999";

        given(resultRepository.findByExamIdAndPhoneNumber(any(), eq(phoneNumber))).willReturn(Optional.empty());

        ExamResult result = resultCommandService.resolveResultForUnregistered(exam, phoneNumber, buildOmrResult(80));

        assertThat(result.getStudentId()).isNull();
        assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(result.getTotalScore()).isEqualTo(80);
        assertThat(result.getQuestionResults()).hasSize(5);
    }

    @Test
    @DisplayName("미등록 학생이 재시험 시 기존 결과를 업데이트한다")
    void resolveResultForUnregistered_existingPhone_regradesResult() {
        Exam exam = buildExam();
        String phoneNumber = "01099999999";
        ExamResult existing = ExamResult.createForUnregistered(exam, phoneNumber, 60);
        for (int i = 1; i <= 5; i++) {
            existing.addQuestionResult(QuestionResult.create(i, "0", String.valueOf(i), 20));
        }

        given(resultRepository.findByExamIdAndPhoneNumber(any(), eq(phoneNumber))).willReturn(Optional.of(existing));

        ExamResult result = resultCommandService.resolveResultForUnregistered(exam, phoneNumber, buildOmrResult(100));

        assertThat(result).isSameAs(existing);
        assertThat(result.getTotalScore()).isEqualTo(100);
    }
}
