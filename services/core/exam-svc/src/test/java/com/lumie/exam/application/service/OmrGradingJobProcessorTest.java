package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
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

import static org.mockito.ArgumentMatchers.any;
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
    private OmrStoragePort storagePort;
    @Mock
    private StudentServicePort studentService;
    @Mock
    private BillingServicePort billingService;
    @Mock
    private OmrGradingJobRepository jobRepository;

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

    private OmrGradingCallbackRequest buildSuccessCallback() {
        return new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123",
                "omr/tmp/abc123/batch1/sheet1.png",
                0, 1,
                true, null,
                "12345678", 100, 1,
                List.of(
                        new OmrGradingCallbackRequest.QuestionResultItem(1, "1", "1", 30, 30, "SHORT"),
                        new OmrGradingCallbackRequest.QuestionResultItem(2, "2", "2", 30, 30, "SHORT"),
                        new OmrGradingCallbackRequest.QuestionResultItem(3, "3", "3", 40, 40, "SHORT")
                )
        );
    }

    @Test
    @DisplayName("콜백 성공 시 결과를 저장하고 incrementSuccess를 호출한다")
    void processGradingCallback_success() {
        Exam exam = buildExam();
        OmrGradingCallbackRequest request = buildSuccessCallback();
        Long studentId = 200L;
        StudentServicePort.StudentInfo student = new StudentServicePort.StudentInfo(
                studentId, "홍길동", "hong@test.com", "01012345678", "루미아카데미", true);
        ExamResult resolvedResult = ExamResult.create(exam, studentId, 100);

        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(studentService.findByPhone("01012345678")).willReturn(Optional.of(student));
        given(resultCommandService.resolveResult(eq(exam), eq(studentId), any())).willReturn(resolvedResult);
        given(storagePort.getObject(any())).willReturn(new byte[]{1, 2, 3});
        given(storagePort.putObject(any(), any(), any())).willReturn("omr/results/abc123/10/200");
        given(resultRepository.save(any(ExamResult.class))).willReturn(resolvedResult);
        given(jobRepository.tryComplete(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(jobRepository).incrementSuccess(1L);
        verify(resultRepository).save(any(ExamResult.class));
        verify(billingService).consumeOmrQuota();
    }

    @Test
    @DisplayName("콜백 실패 시 incrementFail을 호출한다")
    void processGradingCallback_failure() {
        OmrGradingCallbackRequest request = new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123",
                "omr/tmp/abc123/batch1/sheet1.png",
                0, 1,
                false, "OMR 채점 실패",
                null, 0, 0, null
        );

        given(jobRepository.tryComplete(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(jobRepository).incrementFail(1L);
        verify(resultRepository, never()).save(any());
    }
}
