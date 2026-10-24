package com.lumie.assignment.application.service;

import com.lumie.assignment.application.dto.request.CreateAssignmentRequest;
import com.lumie.assignment.application.dto.request.CreateSubmissionRequest;
import com.lumie.assignment.application.dto.request.SubmitAssignmentRequest;
import com.lumie.assignment.application.dto.request.UpdateAssignmentRequest;
import com.lumie.assignment.application.dto.request.UpdateSubmissionRequest;
import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.dto.response.AssignmentSubmissionResultResponse;
import com.lumie.assignment.application.dto.response.SubmissionResponse;
import com.lumie.assignment.application.port.out.AssignmentAudienceReadPort;
import com.lumie.assignment.application.port.out.AssignmentAudienceReadPort.Targets;
import com.lumie.assignment.application.port.out.AssignmentAudienceWritePort;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.common.test.ReflectionTestUtils;
import com.lumie.common.test.TenantContextExtension;
import com.lumie.classroom.api.ClassService;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.vo.AssignmentStatus;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("AssignmentCommandService 테스트")
class AssignmentCommandServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private AssignmentAudienceReadPort audienceReadPort;
    @Mock private AssignmentAudienceWritePort audienceWritePort;
    @Mock private AssignmentRecipientService recipientService;
    @Mock private ClassService classService;
    @Mock private StudentService studentService;
    @Mock private ExamService examService;
    @Mock private AssignmentLifecycleService lifecycleService;
    @InjectMocks private AssignmentCommandService commandService;

    @Nested
    @DisplayName("createAssignment")
    class CreateAssignment {

        @Test
        @DisplayName("정상 생성 시 AssignmentResponse를 반환한다")
        void createAssignment_success() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    1L,
                    futureDueDate(),
                    100,
                    null,
                    null,
                    "EXAM_MANUAL",
                    "EXAM_AUTO",
                    examRequest(),
                    List.of(1L),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null);

            given(classService.getClass(any(), eq(1L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            1L, "수학반", null, null, null, null)));
            given(examService.createExamForAssignment(any(), any())).willReturn(77L);
            given(assignmentRepository.save(any(Assignment.class)))
                    .willAnswer(inv -> {
                        Assignment entity = inv.getArgument(0);
                        ReflectionTestUtils.setField(entity, "id", 1L);
                        return entity;
                    });

            AssignmentResponse response = commandService.createAssignment(request);

            assertThat(response.status()).isEqualTo(AssignmentStatus.ACTIVE);
            assertThat(response.linkedExamId()).isEqualTo(77L);
            var order = inOrder(audienceWritePort, recipientService);
            order.verify(audienceWritePort).createTargets(1L, List.of(1L), List.of());
            order.verify(recipientService).snapshotRecipients("test-tenant", 1L, List.of(1L), List.of());
        }

        @Test
        @DisplayName("반에 등록되지 않은 학생도 직접 대상 과제를 받을 수 있다")
        void createAssignment_directStudentWithoutClass() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    null, futureDueDate(), 100, null, null,
                    "EXAM_MANUAL", "EXAM_AUTO", examRequest(),
                    List.of(), List.of(42L),
                    null, null, null, null, null);

            given(studentService.getStudentsByIds("test-tenant", List.of(42L)))
                    .willReturn(new StudentService.StudentListData(
                            List.of(new StudentService.StudentData(
                                    42L, 142L, "학생", null, true, null, null)),
                            1, 0, 1));
            given(examService.createExamForAssignment(any(), any())).willReturn(77L);
            given(assignmentRepository.save(any(Assignment.class))).willAnswer(inv -> {
                Assignment entity = inv.getArgument(0);
                ReflectionTestUtils.setField(entity, "id", 1L);
                return entity;
            });

            AssignmentResponse response = commandService.createAssignment(request);

            assertThat(response.classId()).isNull();
            assertThat(response.targetStudentIds()).containsExactly(42L);
            verify(classService, never()).getEnrolledClassesForStudents(any(), any());
            verify(recipientService).snapshotRecipients("test-tenant", 1L, List.of(), List.of(42L));
        }

        @Test
        @DisplayName("EXAM_MANUAL 생성은 inline exam payload를 우선해 전용 시험을 만든다")
        void createAssignment_examManualCreatesDedicatedExam() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    1L,
                    futureDueDate(),
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
                            1L, "수학반", null, null, null, null)));
            given(examService.createExamForAssignment(any(), any())).willReturn(77L);
            given(assignmentRepository.save(any(Assignment.class)))
                    .willAnswer(inv -> {
                        Assignment entity = inv.getArgument(0);
                        ReflectionTestUtils.setField(entity, "id", 1L);
                        return entity;
                    });

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
        @DisplayName("마감일이 이미 지난 ACTIVE 과제는 생성 직후 CLOSED가 된다")
        void createAssignment_overdueActive_closesImmediately() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    1L,
                    Instant.parse("2000-01-01T00:00:00Z"),
                    100,
                    null,
                    null,
                    "EXAM_MANUAL",
                    "EXAM_AUTO",
                    examRequest(),
                    List.of(1L),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null);

            given(classService.getClass(any(), eq(1L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            1L, "수학반", null, null, null, null)));
            given(examService.createExamForAssignment(any(), any())).willReturn(77L);
            given(assignmentRepository.save(any(Assignment.class)))
                    .willAnswer(inv -> {
                        Assignment entity = inv.getArgument(0);
                        ReflectionTestUtils.setField(entity, "id", 1L);
                        return entity;
                    });

            AssignmentResponse response = commandService.createAssignment(request);

            assertThat(response.status()).isEqualTo(AssignmentStatus.CLOSED);
        }

        @Test
        @DisplayName("존재하지 않는 수업으로 생성 시 BusinessException이 발생한다")
        void createAssignment_classNotFound_throwsException() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    999L, futureDueDate(), null, null);

            given(classService.getClass(any(), eq(999L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.createAssignment(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("마감일 없이 생성 시 BusinessException이 발생한다")
        void createAssignment_missingDueDate_throwsException() {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    1L, null, null, null);

            assertThatThrownBy(() -> commandService.createAssignment(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(AssignmentErrorCode.DUE_DATE_REQUIRED));
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
            given(audienceReadPort.findByAssignmentId(1L)).willReturn(classTarget(1L));
            given(examService.getExamForAssignment(any(), eq(10L))).willReturn(Optional.of(examData()));

            AssignmentResponse response = commandService.updateAssignment(1L, request);

            assertThat(response.status()).isEqualTo(AssignmentStatus.ACTIVE);
        }

        @Test
        @DisplayName("대상 변경 없는 업데이트는 기존 대상을 삭제 후 재저장하지 않는다")
        void updateAssignment_withoutTargetChange_keepsExistingTargets() throws Exception {
            Assignment entity = buildAssignment(1L);
            UpdateAssignmentRequest request = new UpdateAssignmentRequest(
                    null, null, null, "CLOSED");

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));
            given(assignmentRepository.save(any(Assignment.class))).willAnswer(inv -> inv.getArgument(0));
            given(audienceReadPort.findByAssignmentId(1L)).willReturn(classTarget(1L));
            given(examService.getExamForAssignment(any(), eq(10L))).willReturn(Optional.of(examData()));

            AssignmentResponse response = commandService.updateAssignment(1L, request);

            assertThat(response.status()).isEqualTo(AssignmentStatus.CLOSED);
            assertThat(response.targetClassIds()).containsExactly(1L);
            verify(audienceWritePort, never()).createTargets(any(), any(), any());
        }

        @Test
        @DisplayName("종료된 과제의 마감일을 미래로 연장하면 자동 재개된다")
        void updateAssignment_futureDueDate_reactivatesClosedAssignment() throws Exception {
            Assignment entity = buildAssignment(1L);
            entity.close();
            UpdateAssignmentRequest request = new UpdateAssignmentRequest(
                    Instant.now().plusSeconds(3_600), null, null, null);

            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));
            given(assignmentRepository.save(any(Assignment.class))).willAnswer(inv -> inv.getArgument(0));
            given(audienceReadPort.findByAssignmentId(1L)).willReturn(classTarget(1L));
            given(examService.getExamForAssignment(any(), eq(10L))).willReturn(Optional.of(examData()));

            AssignmentResponse response = commandService.updateAssignment(1L, request);

            assertThat(response.status()).isEqualTo(AssignmentStatus.ACTIVE);
        }

        @Test
        @DisplayName("대상 반 삭제 뒤에도 남은 과제의 마감일을 수정할 수 있다")
        void updateAssignment_deletedTargets_allowsMetadataChange() throws Exception {
            Assignment entity = buildAssignment(1L);
            UpdateAssignmentRequest request = new UpdateAssignmentRequest(
                    Instant.now().plusSeconds(3_600), null, null, null);
            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));
            given(assignmentRepository.save(any(Assignment.class))).willAnswer(inv -> inv.getArgument(0));
            given(audienceReadPort.findByAssignmentId(1L)).willReturn(new Targets(List.of(), List.of()));
            given(examService.getExamForAssignment(any(), eq(10L))).willReturn(Optional.of(examData()));

            AssignmentResponse response = commandService.updateAssignment(1L, request);

            assertThat(response.targetClassIds()).isEmpty();
            verify(audienceWritePort, never()).createTargets(any(), any(), any());
        }

        @Test
        @DisplayName("생성 후 대상 반 변경은 거부한다")
        void updateAssignment_targetChangeIsRejected() throws Exception {
            Assignment entity = buildAssignment(1L);
            UpdateAssignmentRequest request = new UpdateAssignmentRequest(
                    null, null, null, null, null, null, List.of(2L), null,
                    null, null, null, null, null);
            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));
            given(audienceReadPort.findByAssignmentId(1L)).willReturn(classTarget(1L));

            assertThatThrownBy(() -> commandService.updateAssignment(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(AssignmentErrorCode.TARGET_IMMUTABLE));
            verify(audienceWritePort, never()).createTargets(any(), any(), any());
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
            given(audienceReadPort.findByAssignmentId(1L)).willReturn(classTarget(1L));

            assertThatThrownBy(() -> commandService.updateAssignment(1L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("deleteAssignment")
    class DeleteAssignment {

        @Test
        @DisplayName("삭제 시 과제를 실제 삭제한다")
        void deleteAssignment_deletes() throws Exception {
            Assignment entity = buildAssignment(1L);
            given(assignmentRepository.findById(1L)).willReturn(Optional.of(entity));

            commandService.deleteAssignment(1L);

            verify(assignmentRepository).delete(entity);
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
            given(recipientService.eligibleRecipientId(1L, 42L)).willReturn(Optional.of(100L));
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
            assertThat(response.score()).isEqualTo(100);
            assertThat(response.passed()).isTrue();
        }

        @Test
        @DisplayName("중복 제출 시 DuplicateSubmissionException이 발생한다")
        void createSubmission_duplicate_throwsException() throws Exception {
            Assignment assignment = buildAssignment(1L);
            given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));
            given(recipientService.eligibleRecipientId(1L, 42L)).willReturn(Optional.of(100L));
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
            given(recipientService.eligibleRecipientId(1L, 42L)).willReturn(Optional.of(100L));
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
                assertThat(response.submission().passed()).isTrue();
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
            given(recipientService.eligibleRecipientId(1L, 42L)).willReturn(Optional.of(100L));
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
                futureDueDate(),
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

    private Instant futureDueDate() {
        return Instant.parse("2099-04-15T23:59:00Z");
    }

    private Targets classTarget(Long classId) {
        return new Targets(List.of(classId), List.of());
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
