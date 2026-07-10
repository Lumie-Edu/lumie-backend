package com.lumie.assignment.application.service;

import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.dto.response.SubmissionResponse;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.common.exception.BusinessException;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.repository.AssignmentTargetRepository;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.EvaluationMode;
import com.lumie.assignment.domain.vo.SubmissionMode;
import com.lumie.classroom.api.ClassService;
import com.lumie.exam.api.ExamService;
import com.lumie.student.api.StudentService;
import com.lumie.common.test.TenantContextExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("AssignmentQueryService 테스트")
class AssignmentQueryServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private AssignmentTargetRepository targetRepository;
    @Mock private ClassService classService;
    @Mock private StudentService studentService;
    @Mock private ExamService examService;
    @InjectMocks private AssignmentQueryService queryService;

    @Test
    @DisplayName("getAssignment 성공 시 AssignmentResponse를 반환한다")
    void getAssignment_success() throws Exception {
        Assignment entity = buildAssignment(1L);
        given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));

        AssignmentResponse response = queryService.getAssignment(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(AssignmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("getAssignment 존재하지 않으면 AssignmentNotFoundException이 발생한다")
    void getAssignment_notFound_throwsException() {
        given(assignmentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getAssignment(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("listAssignments 성공 시 페이지네이션된 결과를 반환한다")
    void listAssignments_success() throws Exception {
        Assignment entity = buildAssignment(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Assignment> page = new PageImpl<>(List.of(entity), pageable, 1);

        given(assignmentRepository.findAll(any(AssignmentSearchCriteria.class), eq(pageable))).willReturn(page);

        Page<AssignmentResponse> response = queryService.listAssignments(1L, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).status()).isEqualTo(AssignmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("listAssignments 필터 조건을 AssignmentSearchCriteria로 전달한다")
    void listAssignments_withFilters() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Assignment> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(assignmentRepository.findAll(any(AssignmentSearchCriteria.class), eq(pageable))).willReturn(emptyPage);

        Page<AssignmentResponse> response = queryService.listAssignments(
                1L, AssignmentStatus.ACTIVE, pageable);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getAssignmentsByClass 성공 시 해당 수업의 과제 목록을 반환한다")
    void getAssignmentsByClass_success() throws Exception {
        Assignment entity = buildAssignment(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Assignment> page = new PageImpl<>(List.of(entity), pageable, 1);

        given(assignmentRepository.findByTargetClassId(1L, pageable)).willReturn(page);

        Page<AssignmentResponse> response = queryService.getAssignmentsByClass(1L, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getSubmissions 성공 시 제출 목록을 반환한다")
    void getSubmissions_success() throws Exception {
        AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
        submission.submit("답안", null);
        setField(submission, "id", 10L);

        Pageable pageable = PageRequest.of(0, 20);
        Page<AssignmentSubmission> page = new PageImpl<>(List.of(submission), pageable, 1);

        given(submissionRepository.findByAssignmentId(1L, pageable)).willReturn(page);

        Page<SubmissionResponse> response = queryService.getSubmissions(1L, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).studentId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getStudentSubmissions 성공 시 해당 학생의 제출 목록을 반환한다")
    void getStudentSubmissions_success() throws Exception {
        AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
        submission.submit("답안", null);
        setField(submission, "id", 10L);

        Pageable pageable = PageRequest.of(0, 20);
        Page<AssignmentSubmission> page = new PageImpl<>(List.of(submission), pageable, 1);

        given(submissionRepository.findByAssignmentIdAndStudentId(1L, 42L, pageable)).willReturn(page);

        Page<SubmissionResponse> response = queryService.getStudentSubmissions(1L, 42L, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    // -- helpers --

    private Assignment buildAssignment(Long id) throws Exception {
        Assignment entity = Assignment.create(
                1L,
                LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(java.time.ZoneOffset.UTC),
                100,
                null,
                AssignmentStatus.ACTIVE,
                SubmissionMode.EXAM_MANUAL,
                EvaluationMode.EXAM_AUTO,
                10L,
                true,
                true,
                false,
                true,
                false);
        setField(entity, "id", id);
        return entity;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}
