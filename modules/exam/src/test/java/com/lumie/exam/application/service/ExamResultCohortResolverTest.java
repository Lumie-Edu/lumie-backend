package com.lumie.exam.application.service;

import com.lumie.classroom.api.ClassService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.test.ExamFixtures;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ExamResultCohortResolverTest {

    @Mock private StudentService studentService;
    @Mock private ClassService classService;

    private ExamResultCohortResolver resolver;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant("test-academy");
        TenantContextHolder.setTenantId(101L);
        resolver = new ExamResultCohortResolver(studentService, classService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void classFilterTakesPrecedenceOverAggregateEnrollmentFilter() {
        List<ExamResult> results = registeredResults();
        stubStudents(true, true);
        given(classService.getEnrolledStudentIds("test-academy", 101L, 10L)).willReturn(List.of(2L));

        List<ExamResult> filtered = resolver.filter(results, 10L, false, null);

        assertThat(filtered).extracting(ExamResult::getStudentId).containsExactly(2L);
        then(classService).should().getEnrolledStudentIds("test-academy", 101L, 10L);
        then(classService).shouldHaveNoMoreInteractions();
    }

    @Test
    void unassignedFilterKeepsUnregisteredResults() {
        Exam exam = ExamFixtures.aGradedExam();
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 90),
                ExamResult.create(exam, 2L, 70),
                ExamResult.createForUnregistered(exam, "01099998888", 50)
        );
        stubStudents(true, true);
        given(classService.getActiveEnrolledStudentIds("test-academy", 101L)).willReturn(List.of(1L));

        List<ExamResult> filtered = resolver.filter(results, null, false, null);

        assertThat(filtered).extracting(ExamResult::getStudentId).containsExactly(2L, null);
    }

    @Test
    void inactiveFilterExcludesUnregisteredResults() {
        Exam exam = ExamFixtures.aGradedExam();
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 90),
                ExamResult.create(exam, 2L, 70),
                ExamResult.createForUnregistered(exam, "01099998888", 50)
        );
        stubStudents(true, false);

        List<ExamResult> filtered = resolver.filter(results, null, null, false);

        assertThat(filtered).extracting(ExamResult::getStudentId).containsExactly(2L);
        then(classService).shouldHaveNoInteractions();
    }

    private List<ExamResult> registeredResults() {
        Exam exam = ExamFixtures.aGradedExam();
        return List.of(ExamResult.create(exam, 1L, 90), ExamResult.create(exam, 2L, 70));
    }

    private void stubStudents(boolean firstActive, boolean secondActive) {
        given(studentService.getStudentsByIds(anyString(), anyList()))
                .willReturn(new StudentService.StudentListData(List.of(
                        new StudentService.StudentData(1L, null, "학생 1", "010-0000-0001", firstActive, null, null),
                        new StudentService.StudentData(2L, null, "학생 2", "010-0000-0002", secondActive, null, null)
                ), 2, 0, 2));
    }
}
