package com.lumie.assignment.application.service;

import com.lumie.assignment.application.dto.AssignmentResponse;
import com.lumie.assignment.application.dto.CreateAssignmentRequest;
import com.lumie.assignment.application.dto.CreateSubmissionRequest;
import com.lumie.assignment.application.dto.SubmissionResponse;
import com.lumie.assignment.application.dto.UpdateAssignmentRequest;
import com.lumie.assignment.application.dto.UpdateSubmissionRequest;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.classroom.api.ClassService;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.exception.AssignmentNotFoundException;
import com.lumie.assignment.domain.exception.DuplicateSubmissionException;
import com.lumie.assignment.domain.exception.SubmissionNotFoundException;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import com.lumie.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentCommandService 테스트")
class AssignmentCommandServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private ClassService classService;
    @InjectMocks private AssignmentCommandService commandService;

    @org.junit.jupiter.api.BeforeEach
    void setupTenant() {
        TenantContextHolder.setTenant("test-tenant");
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("createAssignment")
    class CreateAssignment {

        @Test
        @DisplayName("정상 생성 시 AssignmentResponse를 반환한다")
        void createAssignment_success() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    1L, "수학 숙제", "1장 풀기", "HOMEWORK",
                    LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC), 100, null);

            given(classService.getClass(any(), eq(1L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            1L, "수학반", null, null, null, null, null, null, null)));
            given(assignmentRepository.save(any(Assignment.class)))
                    .willAnswer(inv -> {
                        Assignment entity = inv.getArgument(0);
                        setField(entity, "id", 1L);
                        return entity;
                    });

            AssignmentResponse response = commandService.createAssignment(request);

            assertThat(response.title()).isEqualTo("수학 숙제");
            assertThat(response.type()).isEqualTo(AssignmentType.HOMEWORK);
            assertThat(response.status()).isEqualTo(AssignmentStatus.ACTIVE);
        }

        @Test
        @DisplayName("존재하지 않는 수업으로 생성 시 BusinessException이 발생한다")
        void createAssignment_classNotFound_throwsException() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    999L, "과제", null, null, null, null, null);

            given(classService.getClass(any(), eq(999L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.createAssignment(request))
                    .isInstanceOf(BusinessException.class);
        }

    }

    @Nested
    @DisplayName("updateAssignment")
    class UpdateAssignment {

        @Test
        @DisplayName("정상 업데이트 시 변경된 AssignmentResponse를 반환한다")
        void updateAssignment_success() throws Exception {
            Assignment entity = buildAssignment(1L);
            UpdateAssignmentRequest request = new UpdateAssignmentRequest(
                    "변경된 제목", null, null, null, null, null, "ACTIVE");

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));
            given(assignmentRepository.save(any(Assignment.class))).willAnswer(inv -> inv.getArgument(0));

            AssignmentResponse response = commandService.updateAssignment(1L, request);

            assertThat(response.title()).isEqualTo("변경된 제목");
            assertThat(response.status()).isEqualTo(AssignmentStatus.ACTIVE);
        }

        @Test
        @DisplayName("존재하지 않는 과제 업데이트 시 AssignmentNotFoundException이 발생한다")
        void updateAssignment_notFound_throwsException() {
            given(assignmentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.updateAssignment(999L,
                    new UpdateAssignmentRequest(null, null, null, null, null, null, null)))
                    .isInstanceOf(AssignmentNotFoundException.class);
        }

        @Test
        @DisplayName("잘못된 상태값으로 업데이트 시 BusinessException이 발생한다")
        void updateAssignment_invalidStatus_throwsException() throws Exception {
            Assignment entity = buildAssignment(1L);
            UpdateAssignmentRequest request = new UpdateAssignmentRequest(
                    null, null, null, null, null, null, "INVALID");

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));

            assertThatThrownBy(() -> commandService.updateAssignment(1L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("deleteAssignment")
    class DeleteAssignment {

        @Test
        @DisplayName("삭제 시 상태가 CLOSED로 변경된다 (소프트 삭제)")
        void deleteAssignment_softDeletes() throws Exception {
            Assignment entity = buildAssignment(1L);
            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));
            given(assignmentRepository.save(any(Assignment.class))).willAnswer(inv -> inv.getArgument(0));

            commandService.deleteAssignment(1L);

            assertThat(entity.getStatus()).isEqualTo(AssignmentStatus.CLOSED);
            verify(assignmentRepository).save(entity);
        }
    }

    @Nested
    @DisplayName("createSubmission")
    class CreateSubmission {

        @Test
        @DisplayName("정상 제출 시 SubmissionResponse를 반환한다")
        void createSubmission_success() throws Exception {
            Assignment assignment = buildAssignment(1L);
            CreateSubmissionRequest request = new CreateSubmissionRequest(42L, "답안 내용", List.of(10L));

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));
            given(submissionRepository.existsByAssignmentIdAndStudentId(1L, 42L)).willReturn(false);
            given(submissionRepository.save(any(AssignmentSubmission.class)))
                    .willAnswer(inv -> {
                        AssignmentSubmission sub = inv.getArgument(0);
                        setField(sub, "id", 1L);
                        return sub;
                    });

            SubmissionResponse response = commandService.createSubmission(1L, request);

            assertThat(response.studentId()).isEqualTo(42L);
            assertThat(response.status()).isEqualTo(SubmissionStatus.SUBMITTED);
            assertThat(response.content()).isEqualTo("답안 내용");
        }

        @Test
        @DisplayName("중복 제출 시 DuplicateSubmissionException이 발생한다")
        void createSubmission_duplicate_throwsException() throws Exception {
            Assignment assignment = buildAssignment(1L);
            given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));
            given(submissionRepository.existsByAssignmentIdAndStudentId(1L, 42L)).willReturn(true);

            assertThatThrownBy(() -> commandService.createSubmission(1L,
                    new CreateSubmissionRequest(42L, "답안", null)))
                    .isInstanceOf(DuplicateSubmissionException.class);
        }
    }

    @Nested
    @DisplayName("updateSubmission")
    class UpdateSubmission {

        @Test
        @DisplayName("채점 시 점수와 피드백이 설정된다")
        void updateSubmission_grade_success() throws Exception {
            AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
            submission.submit("답안", null);
            setField(submission, "id", 10L);

            UpdateSubmissionRequest request = new UpdateSubmissionRequest(85, "잘했습니다.", "GRADED");

            given(submissionRepository.findById(10L)).willReturn(Optional.of(submission));
            given(submissionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            SubmissionResponse response = commandService.updateSubmission(1L, 10L, request);

            assertThat(response.score()).isEqualTo(85);
            assertThat(response.feedback()).isEqualTo("잘했습니다.");
            assertThat(response.status()).isEqualTo(SubmissionStatus.GRADED);
        }

        @Test
        @DisplayName("다른 과제의 submission 수정 시 SubmissionNotFoundException이 발생한다")
        void updateSubmission_wrongAssignment_throwsException() throws Exception {
            AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
            setField(submission, "id", 10L);

            given(submissionRepository.findById(10L)).willReturn(Optional.of(submission));

            assertThatThrownBy(() -> commandService.updateSubmission(999L, 10L,
                    new UpdateSubmissionRequest(null, null, null)))
                    .isInstanceOf(SubmissionNotFoundException.class);
        }
    }

    // -- helpers --

    private Assignment buildAssignment(Long id) throws Exception {
        Assignment entity = Assignment.create(
                1L, "수학 숙제", "1장 풀기",
                AssignmentType.HOMEWORK, LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(java.time.ZoneOffset.UTC),
                100, null);
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
