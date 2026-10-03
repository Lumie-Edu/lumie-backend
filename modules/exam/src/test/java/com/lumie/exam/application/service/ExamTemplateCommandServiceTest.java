package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.exam.application.dto.request.CreateExamTemplateRequest;
import com.lumie.exam.application.dto.request.UpdateExamTemplateRequest;
import com.lumie.exam.domain.entity.ExamTemplate;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamTemplateRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamTemplateCommandServiceTest {
    @Mock ExamTemplateRepository repository;
    @InjectMocks ExamTemplateCommandService service;

    @Test
    void newGradedTemplateKeepsEvaluationConfiguration() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new CreateExamTemplateRequest("상대 5등급", ExamCategory.GRADED,
                GradingType.RELATIVE, GradeScale.FIVE_GRADE, 1, Map.of("1", 10), null, null));

        assertThat(response.gradingType()).isEqualTo(GradingType.RELATIVE);
        assertThat(response.gradeScale()).isEqualTo(GradeScale.FIVE_GRADE);
        assertThat(response.passScore()).isNull();
    }

    @Test
    void newGradedTemplateWithoutConfigurationIsRejected() {
        assertThatThrownBy(() -> service.create(new CreateExamTemplateRequest("구형", ExamCategory.GRADED,
                null, null, 1, Map.of("1", 10), null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.INVALID_EXAM_DATA));
        verify(repository, never()).save(any());
    }

    @Test
    void newPassFailTemplateWithoutCutoffIsRejected() {
        assertThatThrownBy(() -> service.create(new CreateExamTemplateRequest("PNP", ExamCategory.PASS_FAIL,
                null, null, 1, Map.of("1", 10), null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.INVALID_EXAM_DATA));
        verify(repository, never()).save(any());
    }

    @Test
    void newPassFailTemplateWithZeroCutoffIsRejected() {
        assertThatThrownBy(() -> service.create(new CreateExamTemplateRequest("PNP", ExamCategory.PASS_FAIL,
                null, null, 1, Map.of("1", 10), null, 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.INVALID_EXAM_DATA));
        verify(repository, never()).save(any());
    }

    @Test
    void newPassFailTemplateWithCutoffAboveMaximumIsRejected() {
        assertThatThrownBy(() -> service.create(new CreateExamTemplateRequest("PNP", ExamCategory.PASS_FAIL,
                null, null, 1, Map.of("1", 2), null, 3)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.INVALID_EXAM_DATA));
        verify(repository, never()).save(any());
    }

    @Test
    void legacyTemplateNameChangeDoesNotInventConfiguration() {
        ExamTemplate legacy = ExamTemplate.create("기존", ExamCategory.GRADED,
                null, null, 1, Map.of("1", 10), null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(legacy));
        when(repository.save(legacy)).thenReturn(legacy);

        var response = service.update(1L, new UpdateExamTemplateRequest("이름", null,
                null, null, null, null, null, null));

        assertThat(response.name()).isEqualTo("이름");
        assertThat(response.gradingType()).isNull();
        assertThat(response.gradeScale()).isNull();
    }
}
