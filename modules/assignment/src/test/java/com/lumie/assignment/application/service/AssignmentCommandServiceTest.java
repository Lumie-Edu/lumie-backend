package com.lumie.assignment.application.service;

import com.lumie.assignment.application.dto.request.CreateAssignmentRequest;
import com.lumie.assignment.application.dto.request.CreateSubmissionRequest;
import com.lumie.assignment.application.dto.request.SubmitAssignmentRequest;
import com.lumie.assignment.application.dto.request.UpdateAssignmentRequest;
import com.lumie.assignment.application.dto.request.UpdateSubmissionRequest;
import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.dto.response.AssignmentSubmissionResultResponse;
import com.lumie.assignment.application.dto.response.SubmissionResponse;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentTarget;
import com.lumie.common.test.ReflectionTestUtils;
import com.lumie.common.test.TenantContextExtension;
import com.lumie.classroom.api.ClassService;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.repository.AssignmentTargetRepository;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentTargetType;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.api.ExamService;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("AssignmentCommandService 테스트")
class AssignmentCommandServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private AssignmentTargetRepository targetRepository;
    @Mock private ClassService classService;
    @Mock private StudentService studentService;
    @Mock private ExamService examService;
    @InjectMocks private AssignmentCommandService commandService;

    @Nested
    @DisplayName("createAssignment")
    class CreateAssignment {

        @Test
        @DisplayName("정상 생성 시 AssignmentResponse를 반환한다")
        void createAssignment_success() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    1L,
                    LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC),
                    100,
                    null,
                    null,
                    "EXAM_MANUAL",
                    "EXAM_AUTO",
                    examRequest(),
                    null,
                    List.of(1L),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null);

            given(classService.getClass(any(), eq(1L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            1L, "수학반", null, null, null, null, null, null, null)));
            given(examService.createExamForAssignment(any(), any())).willReturn(77L);
            given(assignmentRepository.save(any(Assignment.class)))
                    .willAnswer(inv -> {
                        Assignment entity = inv.getArgument(0);
                        ReflectionTestUtils.setField(entity, "id", 1L);
                        return entity;
                    });
            given(targetRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

            AssignmentResponse response = commandService.createAssignment(request);

            assertThat(response.status()).isEqualTo(AssignmentStatus.ACTIVE);
            assertThat(response.linkedExamId()).isEqualTo(77L);
        }

        @Test
        @DisplayName("EXAM_MANUAL 생성은 inline exam payload를 우선해 전용 시험을 만든다")
        void createAssignment_examManualCreatesDedicatedExam() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    1L,
                    Instant.parse("2026-04-15T23:59:00Z"),
                    100,
                    null,
                    null,
                    "EXAM_MANUAL",
                    "EXAM_AUTO",
                    new com.lumie.assignment.application.dto.request.CreateAssignmentExamRequest(
                            "전용 시험",
                            "GRADED",
                            "ABSOLUTE",
                            "NINE_GRADE",
                            2,
                            Map.of("1", "2", "2", "3"),
                            Map.of("1", 50, "2", 50),
                            Map.of("1", "객관식", "2", "객관식"),
                            60
                    ),
                    999L,
                    List.of(1L),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    true
            );

            given(classService.getClass(any(), eq(1L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            1L, "수학반", null, null, null, null, null, null, null)));
            given(examService.createExamForAssignment(any(), any())).willReturn(77L);
            given(assignmentRepository.save(any(Assignment.class)))
                    .willAnswer(inv -> {
                        Assignment entity = inv.getArgument(0);
                        ReflectionTestUtils.setField(entity, "id", 1L);
                        return entity;
                    });
            given(targetRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

            AssignmentResponse response = commandService.createAssignment(request);

            assertThat(response.linkedExamId()).isEqualTo(77L);
            assertThat(response.showScore()).isTrue();
            assertThat(response.showPassFail()).isTrue();
            assertThat(response.showGrade()).isFalse();
            assertThat(response.showQuestionResults()).isTrue();
            assertThat(response.showCorrectAnswers()).isTrue();
            verify(examService).createExamForAssignment(eq("test-tenant"), any(ExamService.CreateExamData.class));
            verify(examService, never()).getExamForAssignment(any(), eq(999L));
        }

        @Test
        @DisplayName("존재하지 않는 수업으로 생성 시 BusinessException이 발생한다")
        void createAssignment_classNotFound_throwsException() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    999L, null, null, null);

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
                    null, null, null, "ACTIVE");

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));
            given(assignmentRepository.save(any(Assignment.class))).willAnswer(inv -> inv.getArgument(0));
            given(targetRepository.findByAssignmentId(1L)).willReturn(List.of(classTarget(1L)));
            given(classService.getClass(any(), eq(1L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            1L, "수학반", null, null, null, null, null, null, null)));
            given(examService.getExamForAssignment(any(), eq(10L))).willReturn(Optional.of(examData()));
            given(targetRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

            AssignmentResponse response = commandService.updateAssignment(1L, request);

            assertThat(response.status()).isEqualTo(AssignmentStatus.ACTIVE);
        }

        @Test
        @DisplayName("존재하지 않는 과제 업데이트 시 AssignmentNotFoundException이 발생한다")
        void updateAssignment_notFound_throwsException() {
            given(assignmentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.updateAssignment(999L,
                    new UpdateAssignmentRequest(null, null, null, null)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("잘못된 상태값으로 업데이트 시 BusinessException이 발생한다")
        void updateAssignment_invalidStatus_throwsException() throws Exception {
            Assignment entity = buildAssignment(1L);
            UpdateAssignmentRequest request = new UpdateAssignmentRequest(
                    null, null, null, "INVALID");

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));
            given(targetRepository.findByAssignmentId(1L)).willReturn(List.of(classTarget(1L)));
            given(classService.getClass(any(), eq(1L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            1L, "수학반", null, null, null, null, null, null, null)));

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
            CreateSubmissionRequest request = new CreateSubmissionRequest(
                    42L, null, null, Map.of("1", "2", "2", "3"));

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));
            given(submissionRepository.existsByAssignmentIdAndStudentId(1L, 42L)).willReturn(false);
            given(examService.gradeManualAnswers(any(), eq(10L), eq(42L), eq(Map.of("1", "2", "2", "3"))))
                    .willReturn(manualGradeResult());
            given(submissionRepository.save(any(AssignmentSubmission.class)))
                    .willAnswer(inv -> {
                        AssignmentSubmission sub = inv.getArgument(0);
                        ReflectionTestUtils.setField(sub, "id", 1L);
                        return sub;
                    });

            SubmissionResponse response = commandService.createSubmission(1L, request);

            assertThat(response.studentId()).isEqualTo(42L);
            assertThat(response.status()).isEqualTo(SubmissionStatus.GRADED);
            assertThat(response.answers()).containsEntry("1", "2");
            assertThat(response.examResultId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("중복 제출 시 DuplicateSubmissionException이 발생한다")
        void createSubmission_duplicate_throwsException() throws Exception {
            Assignment assignment = buildAssignment(1L);
            given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));
            given(submissionRepository.existsByAssignmentIdAndStudentId(1L, 42L)).willReturn(true);

            assertThatThrownBy(() -> commandService.createSubmission(1L,
                    new CreateSubmissionRequest(42L, "답안", null)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("submitCurrentStudent")
    class SubmitCurrentStudent {

        @Test
        @DisplayName("학생 제출 결과는 정답 공개 여부와 무관하게 문항 유형을 포함한다")
        void submitCurrentStudent_includesQuestionType() throws Exception {
            Assignment assignment = buildAssignment(1L);
            UserContextHolder.setUserId(7L);

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));
            given(studentService.getStudentsByUserIds("test-tenant", List.of(7L)))
                    .willReturn(new StudentService.StudentListData(
                            List.of(new StudentService.StudentData(
                                    42L, 7L, "학생", "01012345678", true, null, null)),
                            1,
                            0,
                            1));
            given(targetRepository.existsByAssignmentIdAndTargetTypeAndTargetId(
                    1L, AssignmentTargetType.STUDENT, 42L)).willReturn(true);
            given(submissionRepository.findFirstByAssignmentIdAndStudentId(1L, 42L))
                    .willReturn(Optional.empty());
            given(examService.gradeManualAnswers(any(), eq(10L), eq(42L), eq(Map.of("1", "2", "2", "3"))))
                    .willReturn(manualGradeResult());
            given(submissionRepository.save(any(AssignmentSubmission.class)))
                    .willAnswer(inv -> {
                        AssignmentSubmission sub = inv.getArgument(0);
                        ReflectionTestUtils.setField(sub, "id", 1L);
                        return sub;
                    });

            try {
                AssignmentSubmissionResultResponse response = commandService.submitCurrentStudent(1L,
                        new SubmitAssignmentRequest(null, null, Map.of("1", "2", "2", "3")));

                assertThat(response.questionResults()).hasSize(1);
                assertThat(response.questionResults().get(0).questionType()).isEqualTo("객관식");
                assertThat(response.questionResults().get(0).correctAnswer()).isNull();
            } finally {
                UserContextHolder.clear();
            }
        }

        @Test
        @DisplayName("이미 제출한 과제는 재제출할 수 없다")
        void submitCurrentStudent_duplicate_throwsException() throws Exception {
            Assignment assignment = buildAssignment(1L);
            AssignmentSubmission existing = AssignmentSubmission.create(1L, 42L);
            UserContextHolder.setUserId(7L);

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));
            given(studentService.getStudentsByUserIds("test-tenant", List.of(7L)))
                    .willReturn(new StudentService.StudentListData(
                            List.of(new StudentService.StudentData(
                                    42L, 7L, "학생", "01012345678", true, null, null)),
                            1,
                            0,
                            1));
            given(targetRepository.existsByAssignmentIdAndTargetTypeAndTargetId(
                    1L, AssignmentTargetType.STUDENT, 42L)).willReturn(true);
            given(submissionRepository.findFirstByAssignmentIdAndStudentId(1L, 42L))
                    .willReturn(Optional.of(existing));

            try {
                assertThatThrownBy(() -> commandService.submitCurrentStudent(1L,
                        new SubmitAssignmentRequest(null, null, Map.of("1", "2"))))
                        .isInstanceOf(BusinessException.class);
            } finally {
                UserContextHolder.clear();
            }
            verify(examService, never()).gradeManualAnswers(any(), any(), any(), any());
            verify(submissionRepository, never()).save(any(AssignmentSubmission.class));
        }
    }

    @Nested
    @DisplayName("updateSubmission")
    class UpdateSubmission {

        @Test
        @DisplayName("시험 연동 과제는 제출 수동 채점을 허용하지 않는다")
        void updateSubmission_examAuto_throwsException() throws Exception {
            AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
            submission.submit("답안", null);
            ReflectionTestUtils.setField(submission, "id", 10L);

            UpdateSubmissionRequest request = new UpdateSubmissionRequest(85, "잘했습니다.", "GRADED");

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(buildAssignment(1L)));
            given(submissionRepository.findById(10L)).willReturn(Optional.of(submission));

            assertThatThrownBy(() -> commandService.updateSubmission(1L, 10L, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("다른 과제의 submission 수정 시 SubmissionNotFoundException이 발생한다")
        void updateSubmission_wrongAssignment_throwsException() throws Exception {
            AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
            ReflectionTestUtils.setField(submission, "id", 10L);

            given(assignmentRepository.findById(999L)).willReturn(Optional.of(buildAssignment(999L)));
            given(submissionRepository.findById(10L)).willReturn(Optional.of(submission));

            assertThatThrownBy(() -> commandService.updateSubmission(999L, 10L,
                    new UpdateSubmissionRequest(null, null, null)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // -- helpers --

    private Assignment buildAssignment(Long id) throws Exception {
        Assignment entity = Assignment.create(
                1L,
                LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(java.time.ZoneOffset.UTC),
                100,
                null,
                AssignmentStatus.ACTIVE,
                com.lumie.assignment.domain.vo.SubmissionMode.EXAM_MANUAL,
                com.lumie.assignment.domain.vo.EvaluationMode.EXAM_AUTO,
                10L,
                true,
                true,
                false,
                true,
                false);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private AssignmentTarget classTarget(Long classId) {
        return AssignmentTarget.create(1L, AssignmentTargetType.CLASS, classId);
    }

    private com.lumie.assignment.application.dto.request.CreateAssignmentExamRequest examRequest() {
        return new com.lumie.assignment.application.dto.request.CreateAssignmentExamRequest(
                "전용 시험",
                "GRADED",
                "ABSOLUTE",
                "NINE_GRADE",
                2,
                Map.of("1", "2", "2", "3"),
                Map.of("1", 50, "2", 50),
                Map.of("1", "객관식", "2", "객관식"),
                60
        );
    }

    private ExamService.ExamAssignmentData examData() {
        return new ExamService.ExamAssignmentData(
                10L,
                "전용 시험",
                "GRADED",
                "ABSOLUTE",
                "NINE_GRADE",
                2,
                100,
                null,
                List.of());
    }

    private ExamService.ManualGradeResult manualGradeResult() {
        return new ExamService.ManualGradeResult(
                500L,
                10L,
                42L,
                100,
                100,
                null,
                true,
                List.of(new ExamService.QuestionGradeData(900L, 1, "2", true, 50, 50, "2", "객관식")));
    }

}
