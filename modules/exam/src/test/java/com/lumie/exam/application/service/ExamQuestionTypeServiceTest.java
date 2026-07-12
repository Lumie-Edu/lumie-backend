package com.lumie.exam.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.dto.request.CreateExamQuestionTypeRequest;
import com.lumie.exam.application.dto.response.ExamQuestionTypeResponse;
import com.lumie.exam.application.port.out.ExamQuestionTypePort;
import com.lumie.exam.domain.entity.ExamQuestionType;
import com.lumie.exam.domain.exception.ExamErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamQuestionTypeServiceTest {

    @Mock
    private ExamQuestionTypePort examQuestionTypePort;

    @InjectMocks
    private ExamQuestionTypeService examQuestionTypeService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void listQuestionTypesReturnsDisplayOrder() {
        ExamQuestionType first = ExamQuestionType.create("듣기", 1);
        ExamQuestionType second = ExamQuestionType.create("빈칸", 2);
        when(examQuestionTypePort.findAllOrdered())
                .thenReturn(List.of(first, second));

        List<ExamQuestionTypeResponse> result = examQuestionTypeService.listQuestionTypes();

        assertThat(result).extracting(ExamQuestionTypeResponse::name)
                .containsExactly("듣기", "빈칸");
    }

    @Test
    void createQuestionTypeTrimsNameAndAppendsOrder() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        when(examQuestionTypePort.existsByName("어휘")).thenReturn(false);
        when(examQuestionTypePort.findMaxDisplayOrder()).thenReturn(9);
        when(examQuestionTypePort.save(any(ExamQuestionType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        examQuestionTypeService.createQuestionType(new CreateExamQuestionTypeRequest(" 어휘 "));

        ArgumentCaptor<ExamQuestionType> captor = ArgumentCaptor.forClass(ExamQuestionType.class);
        verify(examQuestionTypePort).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("어휘");
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(10);
    }

    @Test
    void createQuestionTypeRejectsDuplicateName() {
        UserContextHolder.setUserRole(Role.MANAGER);
        when(examQuestionTypePort.existsByName("듣기")).thenReturn(true);

        assertThatThrownBy(() -> examQuestionTypeService.createQuestionType(new CreateExamQuestionTypeRequest("듣기")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.DUPLICATE_QUESTION_TYPE);

        verify(examQuestionTypePort, never()).save(any());
    }

    @Test
    void createQuestionTypeTranslatesUniqueConstraintRace() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        when(examQuestionTypePort.existsByName("듣기")).thenReturn(false);
        when(examQuestionTypePort.findMaxDisplayOrder()).thenReturn(9);
        when(examQuestionTypePort.save(any(ExamQuestionType.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "uq_exam_question_types_tenant_name",
                        new SQLException("unique violation", "23505")
                ));

        assertThatThrownBy(() -> examQuestionTypeService.createQuestionType(new CreateExamQuestionTypeRequest("듣기")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.DUPLICATE_QUESTION_TYPE);
    }

    @Test
    void createQuestionTypeLetsUnexpectedIntegrityErrorsBubble() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        when(examQuestionTypePort.existsByName("듣기")).thenReturn(false);
        when(examQuestionTypePort.findMaxDisplayOrder()).thenReturn(9);
        DataIntegrityViolationException unexpected = new DataIntegrityViolationException("not null");
        when(examQuestionTypePort.save(any(ExamQuestionType.class))).thenThrow(unexpected);

        assertThatThrownBy(() -> examQuestionTypeService.createQuestionType(new CreateExamQuestionTypeRequest("듣기")))
                .isSameAs(unexpected);
    }

    @Test
    void deleteQuestionTypeRejectsMissingRow() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        when(examQuestionTypePort.deleteById(1L)).thenReturn(false);

        assertThatThrownBy(() -> examQuestionTypeService.deleteQuestionType(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.QUESTION_TYPE_NOT_FOUND);

        verify(examQuestionTypePort).deleteById(1L);
    }

    @Test
    void createQuestionTypeRejectsStudentUser() {
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> examQuestionTypeService.createQuestionType(new CreateExamQuestionTypeRequest("장문독해")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.FORBIDDEN);

        verify(examQuestionTypePort, never()).save(any());
    }

    @Test
    void deleteQuestionTypeRejectsStudentUser() {
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> examQuestionTypeService.deleteQuestionType(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.FORBIDDEN);

        verify(examQuestionTypePort, never()).deleteById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedDefaultQuestionTypesIfEmptyCreatesDefaults() {
        when(examQuestionTypePort.findAllByNameIn(any())).thenReturn(List.of());

        examQuestionTypeService.seedDefaultQuestionTypesIfEmpty();

        ArgumentCaptor<List<ExamQuestionType>> captor = ArgumentCaptor.forClass(List.class);
        verify(examQuestionTypePort).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(9);
        assertThat(captor.getValue()).extracting(ExamQuestionType::getName)
                .containsExactly("듣기", "기초유형", "내용파악", "빈칸", "순서", "삽입", "어법", "어휘", "단어");
    }

    @Test
    void seedDefaultQuestionTypesIfEmptyDoesNotRestoreDeletedDefaults() {
        when(examQuestionTypePort.findAllByNameIn(any()))
                .thenReturn(List.of(ExamQuestionType.create("듣기", 1), ExamQuestionType.create("단어", 9)));

        examQuestionTypeService.seedDefaultQuestionTypesIfEmpty();

        verify(examQuestionTypePort, never()).saveAll(any());
    }

    @Test
    void seedDefaultQuestionTypesIfEmptyKeepsCompleteDefaults() {
        List<ExamQuestionType> defaults = List.of(
                ExamQuestionType.create("듣기", 1),
                ExamQuestionType.create("기초유형", 2),
                ExamQuestionType.create("내용파악", 3),
                ExamQuestionType.create("빈칸", 4),
                ExamQuestionType.create("순서", 5),
                ExamQuestionType.create("삽입", 6),
                ExamQuestionType.create("어법", 7),
                ExamQuestionType.create("어휘", 8),
                ExamQuestionType.create("단어", 9)
        );
        when(examQuestionTypePort.findAllByNameIn(any())).thenReturn(defaults);

        examQuestionTypeService.seedDefaultQuestionTypesIfEmpty();

        verify(examQuestionTypePort, never()).saveAll(any());
    }
}
