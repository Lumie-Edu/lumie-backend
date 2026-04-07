package com.lumie.exam.application.service;

import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OmrGradingJobProcessor 단위 테스트")
class OmrGradingJobProcessorTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamResultRepository resultRepository;
    @Mock
    private ResultCommandService resultCommandService;
    @Mock
    private OmrServicePort omrService;
    @Mock
    private OmrStoragePort storagePort;
    @Mock
    private StudentServicePort studentService;
    @Mock
    private BillingServicePort billingService;

    @InjectMocks
    private OmrGradingJobProcessor processor;

    private Exam buildExam() {
        return Exam.create(
                "배치 테스트 시험",
                ExamCategory.GRADED,
                GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE,
                3,
                Map.of("1", "1", "2", "2", "3", "3"),
                Map.of("1", 30, "2", 30, "3", 40),
                Map.of("1", "SHORT", "2", "SHORT", "3", "SHORT"),
                null
        );
    }

    private OmrServicePort.OmrGradingResult buildOmrResult(String phoneNumber, int totalScore) {
        List<OmrServicePort.OmrQuestionResult> questions = List.of(
                new OmrServicePort.OmrQuestionResult(1, "1", "1", 30, 30, "SHORT"),
                new OmrServicePort.OmrQuestionResult(2, "2", "2", 30, 30, "SHORT"),
                new OmrServicePort.OmrQuestionResult(3, "3", "3", 40, 40, "SHORT")
        );
        return new OmrServicePort.OmrGradingResult(totalScore, 1, phoneNumber, questions);
    }

    @Test
    @DisplayName("채점 성공 시 resolveResult를 통해 결과를 저장한다")
    void processSingleImage_success_savesResult() {
        // given
        Long jobId = 1L;
        Long examId = 10L;
        Long studentId = 200L;
        String imageKey = "omr/tmp/abc123/batch1/sheet1.png";
        String tenantSlug = "inst-abc123";

        Exam exam = buildExam();
        StudentServicePort.StudentInfo student = new StudentServicePort.StudentInfo(
                studentId, "홍길동", "hong@test.com", "01012345678", "루미아카데미", true);
        ExamResult resolvedResult = ExamResult.create(exam, studentId, 100);

        given(examRepository.findById(examId)).willReturn(Optional.of(exam));
        given(storagePort.getObject(imageKey)).willReturn(new byte[]{1, 2, 3});
        given(omrService.gradeOmrImage(any(), any(), any(), any()))
                .willReturn(buildOmrResult("12345678", 100));
        given(studentService.findByPhone("01012345678")).willReturn(Optional.of(student));
        given(resultCommandService.resolveResult(eq(exam), eq(studentId), any())).willReturn(resolvedResult);
        given(resultRepository.save(any(ExamResult.class))).willReturn(resolvedResult);
        given(storagePort.putObject(any(), any(), any())).willReturn("omr/results/abc123/10/200");

        // when
        Map<String, Object> result = processor.processSingleImage(jobId, examId, imageKey, tenantSlug);

        // then
        verify(resultCommandService).resolveResult(eq(exam), eq(studentId), any());
        verify(resultRepository).save(any(ExamResult.class));
        assertThat(result).containsEntry("saved", true);
        assertThat(result).containsEntry("success", true);
    }

    @Test
    @DisplayName("미등록 학생일 때 phoneNumber로 결과를 저장한다")
    void processSingleImage_studentNotFound_savesAsUnregistered() {
        // given
        Exam exam = buildExam();
        ExamResult unregisteredResult = ExamResult.createForUnregistered(exam, "01099999999", 70);

        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(storagePort.getObject(any())).willReturn(new byte[]{1, 2, 3});
        given(omrService.gradeOmrImage(any(), any(), any(), any()))
                .willReturn(buildOmrResult("99999999", 70));
        given(studentService.findByPhone("01099999999")).willReturn(Optional.empty());
        given(resultCommandService.resolveResultForUnregistered(eq(exam), eq("01099999999"), any()))
                .willReturn(unregisteredResult);
        given(resultRepository.save(any(ExamResult.class))).willReturn(unregisteredResult);
        given(storagePort.putObject(any(), any(), any())).willReturn("omr/results/abc123/10/01099999999");

        // when
        Map<String, Object> result = processor.processSingleImage(
                1L, 10L, "omr/tmp/abc123/batch1/sheet1.png", "inst-abc123");

        // then
        verify(resultCommandService).resolveResultForUnregistered(eq(exam), eq("01099999999"), any());
        verify(resultRepository).save(any(ExamResult.class));
        assertThat(result).containsEntry("saved", true);
        assertThat(result).containsEntry("success", true);
        assertThat(result).doesNotContainKey("studentId");
    }

    @Test
    @DisplayName("시험이 존재하지 않으면 ExamException을 던진다")
    void processSingleImage_examNotFound_throwsExamException() {
        given(examRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> processor.processSingleImage(
                1L, 99L, "omr/tmp/abc123/batch1/sheet1.png", "inst-abc123"))
                .isInstanceOf(ExamException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.EXAM_NOT_FOUND);
    }

    @Test
    @DisplayName("이미지 처리 예외 발생 시 실패로 기록하고 결과를 반환한다")
    void processSingleImage_storageException_recordsFailure() {
        // given
        Exam exam = buildExam();

        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(storagePort.getObject(any())).willThrow(new RuntimeException("Storage unavailable"));

        // when
        Map<String, Object> result = processor.processSingleImage(
                1L, 10L, "omr/tmp/abc123/batch1/sheet1.png", "inst-abc123");

        // then
        verify(resultRepository, never()).save(any());
        assertThat(result).containsEntry("success", false);
        assertThat(result).containsEntry("saved", false);
        assertThat(result.get("error")).isEqualTo("Storage unavailable");
    }
}
