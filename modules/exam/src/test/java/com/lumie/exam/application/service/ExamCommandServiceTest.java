package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.api.ExamResultChangedEvent;
import com.lumie.exam.application.dto.request.UpdateExamRequest;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamCommandServiceTest {
    @Mock ExamRepository examRepository;
    @Mock ExamResultRepository resultRepository;
    @Spy GradeCalculator gradeCalculator;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks ExamCommandService service;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void changingCutoffReprojectsExistingPassFailResult() {
        TenantContextHolder.setTenant("test-academy");
        TenantContextHolder.setTenantId(101L);
        Exam exam = passFailExam();
        ExamResult result = ExamResult.create(exam, 7L, 6);
        result.assignGrade(-1);
        result.addQuestionResult(QuestionResult.create(1, "1", "1", 6));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(1L)).thenReturn(List.of(result));
        when(examRepository.save(exam)).thenReturn(exam);

        service.updateExam(1L, new UpdateExamRequest(null, null, null, null,
                null, null, null, null, 7));

        assertThat(exam.getPassScore()).isEqualTo(7);
        assertThat(result.getGrade()).isEqualTo(-1);
        ArgumentCaptor<ExamResultChangedEvent> events = ArgumentCaptor.forClass(ExamResultChangedEvent.class);
        verify(eventPublisher).publishEvent(events.capture());
        assertThat(events.getValue().passed()).isFalse();
        assertThat(events.getValue().score()).isEqualTo(6);
        assertThat(events.getValue().answers()).containsEntry("1", "1");
        assertThat(events.getValue().grade()).isNull();
    }

    @Test
    void changingAnswerKeyWithResultsIsRejected() {
        Exam exam = passFailExam();
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(1L)).thenReturn(List.of(ExamResult.create(exam, 7L, 6)));

        assertThatThrownBy(() -> service.updateExam(1L, new UpdateExamRequest(null, null, null,
                null, null, Map.of("1", "2"), null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.INVALID_EXAM_DATA));
        verify(examRepository, never()).save(any());
    }

    @Test
    void zeroPassFailCutoffIsRejected() {
        Exam exam = passFailExam();
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateExam(1L, new UpdateExamRequest(null, null, null,
                null, null, null, null, null, 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.INVALID_EXAM_DATA));
        verify(examRepository, never()).save(any());
    }

    @Test
    void passFailCutoffAboveMaximumScoreIsRejected() {
        Exam exam = passFailExam();
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateExam(1L, new UpdateExamRequest(null, null, null,
                null, null, null, null, null, 11)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.INVALID_EXAM_DATA));
        verify(examRepository, never()).save(any());
    }

    private Exam passFailExam() {
        return Exam.createForAssignment("PNP", ExamCategory.PASS_FAIL, GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE, 1, Map.of("1", "1"), Map.of("1", 10),
                Map.of("1", "CHOICE"), 5);
    }
}
