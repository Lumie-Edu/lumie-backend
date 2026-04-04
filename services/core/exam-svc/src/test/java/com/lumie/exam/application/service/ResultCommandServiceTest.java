package com.lumie.exam.application.service;

import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.service.GradeCalculator;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private StudentServicePort studentService;
    @Mock
    private BillingServicePort billingService;
    @Mock
    private OmrServicePort omrService;
    @Mock
    private OmrStoragePort storagePort;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ResultCommandService resultCommandService;

    private Exam buildExam() {
        return Exam.create(
                "테스트 시험",
                ExamCategory.GRADED,
                GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE,
                5,
                Map.of("1", "1", "2", "2", "3", "3", "4", "4", "5", "5"),
                Map.of("1", 20, "2", 20, "3", 20, "4", 20, "5", 20),
                Map.of("1", "SHORT", "2", "SHORT", "3", "SHORT", "4", "SHORT", "5", "SHORT"),
                null
        );
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

    @Test
    @DisplayName("신규 학생이면 새 결과를 생성하여 저장한다")
    void processOmrGrading_newStudent_createsNewResult() {
        // given
        Long examId = 1L;
        Long studentId = 100L;
        Exam exam = buildExam();
        ExamResult savedResult = ExamResult.create(exam, studentId, 100);

        given(examRepository.findById(examId)).willReturn(Optional.of(exam));
        given(studentService.existsById(studentId)).willReturn(true);
        given(billingService.hasOmrQuota()).willReturn(true);
        given(omrService.gradeOmrImage(any(), any(), any(), any())).willReturn(buildOmrResult(100));
        given(resultRepository.findByExamIdAndStudentId(examId, studentId)).willReturn(Optional.empty());
        given(resultRepository.save(any(ExamResult.class))).willReturn(savedResult);

        // when
        resultCommandService.processOmrGrading(examId, studentId, new byte[]{1, 2, 3});

        // then
        verify(resultRepository).findByExamIdAndStudentId(examId, studentId);
        verify(resultRepository).save(any(ExamResult.class));
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
        given(studentService.existsById(studentId)).willReturn(true);
        given(billingService.hasOmrQuota()).willReturn(true);
        given(omrService.gradeOmrImage(any(), any(), any(), any())).willReturn(buildOmrResult(100));
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
                .isInstanceOf(ExamException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.EXAM_NOT_FOUND);
    }

    @Test
    @DisplayName("학생이 존재하지 않으면 ExamException을 던진다")
    void processOmrGrading_studentNotFound_throwsExamException() {
        given(examRepository.findById(1L)).willReturn(Optional.of(buildExam()));
        given(studentService.existsById(100L)).willReturn(false);

        assertThatThrownBy(() -> resultCommandService.processOmrGrading(1L, 100L, new byte[]{1}))
                .isInstanceOf(ExamException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.STUDENT_NOT_FOUND);
    }

    @Test
    @DisplayName("OMR 쿼터가 초과되면 ExamException을 던진다")
    void processOmrGrading_quotaExceeded_throwsExamException() {
        given(examRepository.findById(1L)).willReturn(Optional.of(buildExam()));
        given(studentService.existsById(100L)).willReturn(true);
        given(billingService.hasOmrQuota()).willReturn(false);

        assertThatThrownBy(() -> resultCommandService.processOmrGrading(1L, 100L, new byte[]{1}))
                .isInstanceOf(ExamException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.OMR_QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("OMR 채점 서비스 실패 시 ExamException을 던진다")
    void processOmrGrading_omrServiceFails_throwsExamException() {
        given(examRepository.findById(1L)).willReturn(Optional.of(buildExam()));
        given(studentService.existsById(100L)).willReturn(true);
        given(billingService.hasOmrQuota()).willReturn(true);
        given(omrService.gradeOmrImage(any(), any(), any(), any()))
                .willThrow(new RuntimeException("OMR service unavailable"));

        assertThatThrownBy(() -> resultCommandService.processOmrGrading(1L, 100L, new byte[]{1}))
                .isInstanceOf(ExamException.class)
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
        given(studentService.existsById(studentId)).willReturn(true);
        given(billingService.hasOmrQuota()).willReturn(true);
        given(omrService.gradeOmrImage(any(), any(), any(), any())).willReturn(buildOmrResult(100));
        given(resultRepository.findByExamIdAndStudentId(examId, studentId)).willReturn(Optional.empty());
        given(resultRepository.save(any(ExamResult.class))).willReturn(savedResult);

        resultCommandService.processOmrGrading(examId, studentId, new byte[]{1});

        verify(billingService).consumeOmrQuota();
    }
}
