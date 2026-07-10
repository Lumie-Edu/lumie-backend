package com.lumie.assignment.application.service;

import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.dto.response.AssignmentQuestionResultResponse;
import com.lumie.assignment.application.dto.response.AssignmentSubmissionStatusResponse;
import com.lumie.assignment.application.dto.response.ExamAssignmentResponse;
import com.lumie.assignment.application.dto.response.StudentAssignmentDetailResponse;
import com.lumie.assignment.application.dto.response.SubmissionResponse;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.entity.AssignmentTarget;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.repository.AssignmentTargetRepository;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentTargetType;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import com.lumie.assignment.domain.vo.SubmissionMode;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.api.ExamService;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentQueryService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentTargetRepository targetRepository;
    private final ClassService classService;
    private final StudentService studentService;
    private final ExamService examService;

    public AssignmentResponse getAssignment(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                "Assignment not found with id: %d".formatted(id)));
        return toAssignmentResponse(assignment, targetRepository.findByAssignmentId(id));
    }

    public Page<AssignmentResponse> listAssignments(Long classId, AssignmentStatus status,
                                                     Pageable pageable) {
        AssignmentSearchCriteria criteria = new AssignmentSearchCriteria(classId, status);
        Page<Assignment> page = assignmentRepository.findAll(criteria, pageable);
        Map<Long, List<AssignmentTarget>> targetsByAssignment = targetsByAssignment(page.getContent());
        return page.map(assignment -> AssignmentResponse.from(
                assignment,
                targetsByAssignment.getOrDefault(assignment.getId(), List.of()),
                examName(assignment)));
    }

    public Page<AssignmentResponse> getAssignmentsByClass(Long classId, Pageable pageable) {
        Page<Assignment> page = assignmentRepository.findByTargetClassId(classId, pageable);
        Map<Long, List<AssignmentTarget>> targetsByAssignment = targetsByAssignment(page.getContent());
        return page.map(assignment -> AssignmentResponse.from(
                assignment,
                targetsByAssignment.getOrDefault(assignment.getId(), List.of()),
                examName(assignment)));
    }

    public Page<SubmissionResponse> getSubmissions(Long assignmentId, Pageable pageable) {
        return submissionRepository.findByAssignmentId(assignmentId, pageable)
            .map(SubmissionResponse::from);
    }

    public List<AssignmentSubmissionStatusResponse> getSubmissionStatuses(Long assignmentId) {
        var ignored = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                        "Assignment not found with id: %d".formatted(assignmentId)));

        String slug = TenantContextHolder.getRequiredTenant();
        List<AssignmentTarget> targets = targetRepository.findByAssignmentId(assignmentId);
        Set<Long> studentIds = new LinkedHashSet<>();
        targets.stream()
                .filter(target -> target.getTargetType() == AssignmentTargetType.STUDENT)
                .map(AssignmentTarget::getTargetId)
                .forEach(studentIds::add);
        targets.stream()
                .filter(target -> target.getTargetType() == AssignmentTargetType.CLASS)
                .map(AssignmentTarget::getTargetId)
                .forEach(classId -> studentIds.addAll(classService.getEnrolledStudentIds(slug, classId)));

        Map<Long, AssignmentSubmission> submissionsByStudent = submissionRepository.findAllByAssignmentId(assignmentId)
                .stream()
                .filter(submission -> submission.getStudentId() != null)
                .collect(Collectors.toMap(
                        AssignmentSubmission::getStudentId,
                        submission -> submission,
                        (left, right) -> right));
        Map<Long, String> studentNamesById = studentIds.isEmpty()
                ? Map.of()
                : studentService.getStudentsByIds(slug, List.copyOf(studentIds)).students().stream()
                        .filter(student -> student.id() != null)
                        .filter(student -> student.studentName() != null && !student.studentName().isBlank())
                        .collect(Collectors.toMap(
                                StudentService.StudentData::id,
                                StudentService.StudentData::studentName,
                                (left, right) -> left));

        return studentIds.stream()
                .map(studentId -> {
                    AssignmentSubmission submission = submissionsByStudent.get(studentId);
                    return new AssignmentSubmissionStatusResponse(
                            studentId,
                            studentNamesById.get(studentId),
                            submission != null ? submission.getStatus() : SubmissionStatus.PENDING,
                            submission != null ? SubmissionResponse.from(submission) : null);
                })
                .toList();
    }

    public Page<SubmissionResponse> getStudentSubmissions(Long assignmentId, Long studentId, Pageable pageable) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId, pageable)
            .map(SubmissionResponse::from);
    }

    public List<AssignmentResponse> getCurrentStudentAssignments() {
        Long studentId = currentStudentId();
        List<Long> classIds = currentStudentClassIds(studentId);
        List<Long> visibleAssignmentIds = targetRepository.findVisibleAssignmentIds(
                studentId,
                classIds.isEmpty() ? List.of(-1L) : classIds);
        if (visibleAssignmentIds.isEmpty()) {
            return List.of();
        }

        List<Assignment> assignments = assignmentRepository
                .findByIdIn(visibleAssignmentIds)
                .stream()
                .filter(assignment -> assignment.getSubmissionMode() == SubmissionMode.EXAM_MANUAL)
                .sorted(Comparator
                        .comparing(Assignment::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Assignment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        Map<Long, List<AssignmentTarget>> targetsByAssignment = targetsByAssignment(assignments);
        return assignments.stream()
                .map(assignment -> studentAssignmentResponse(assignment, examName(assignment)))
                .toList();
    }

    public StudentAssignmentDetailResponse getCurrentStudentAssignment(Long assignmentId) {
        Long studentId = currentStudentId();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                        "Assignment not found with id: %d".formatted(assignmentId)));
        if (!isTargetedToStudent(assignmentId, studentId)) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_VISIBLE);
        }

        List<AssignmentTarget> targets = targetRepository.findByAssignmentId(assignmentId);
        AssignmentSubmission submission = submissionRepository
                .findFirstByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElse(null);
        ExamAssignmentResponse exam = assignment.getSubmissionMode() == SubmissionMode.EXAM_MANUAL
                ? examService.getExamForAssignment(TenantContextHolder.getRequiredTenant(), assignment.getLinkedExamId())
                        .map(this::toExamResponse)
                        .orElse(null)
                : null;

        return new StudentAssignmentDetailResponse(
                studentAssignmentResponse(assignment, exam != null ? exam.name() : examName(assignment)),
                assignment.getStatus() == AssignmentStatus.ACTIVE,
                submission != null ? studentSubmissionResponse(assignment, submission) : null,
                exam,
                existingQuestionResults(assignment, submission)
        );
    }

    private boolean isTargetedToStudent(Long assignmentId, Long studentId) {
        if (targetRepository.existsByAssignmentIdAndTargetTypeAndTargetId(
                assignmentId, AssignmentTargetType.STUDENT, studentId)) {
            return true;
        }
        return currentStudentClassIds(studentId).stream()
                .anyMatch(classId -> targetRepository.existsByAssignmentIdAndTargetTypeAndTargetId(
                        assignmentId, AssignmentTargetType.CLASS, classId));
    }

    private AssignmentResponse toAssignmentResponse(Assignment assignment, List<AssignmentTarget> targets) {
        return AssignmentResponse.from(assignment, targets, examName(assignment));
    }

    private AssignmentResponse studentAssignmentResponse(Assignment assignment, String examName) {
        return new AssignmentResponse(
                assignment.getId(),
                null,
                assignment.getDueDate(),
                assignment.getMaxScore(),
                assignment.getAttachments(),
                assignment.getStatus(),
                assignment.getSubmissionMode(),
                assignment.getEvaluationMode(),
                assignment.getLinkedExamId(),
                examName,
                List.of(),
                List.of(),
                assignment.isShowScore(),
                assignment.isShowPassFail(),
                assignment.isShowGrade(),
                assignment.isShowQuestionResults(),
                assignment.isShowCorrectAnswers(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    private String examName(Assignment assignment) {
        if (assignment.getLinkedExamId() == null) {
            return null;
        }
        return examService.getExamForAssignment(TenantContextHolder.getRequiredTenant(), assignment.getLinkedExamId())
                .map(ExamService.ExamAssignmentData::name)
                .orElse(null);
    }

    private Long currentStudentId() {
        String slug = TenantContextHolder.getRequiredTenant();
        Long userId = UserContextHolder.getRequiredUserId();
        return studentService.getStudentsByUserIds(slug, List.of(userId)).students().stream()
                .findFirst()
                .map(StudentService.StudentData::id)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.STUDENT_NOT_FOUND));
    }

    private List<Long> currentStudentClassIds(Long studentId) {
        return classService.getEnrolledClassesForStudents(TenantContextHolder.getRequiredTenant(), List.of(studentId))
                .stream()
                .map(ClassService.EnrolledClassData::classId)
                .distinct()
                .toList();
    }

    private Map<Long, List<AssignmentTarget>> targetsByAssignment(List<Assignment> assignments) {
        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .toList();
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        return targetRepository.findByAssignmentIdIn(assignmentIds).stream()
                .collect(Collectors.groupingBy(AssignmentTarget::getAssignmentId));
    }

    private SubmissionResponse studentSubmissionResponse(Assignment assignment, AssignmentSubmission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignmentId(),
                submission.getStudentId(),
                submission.getContent(),
                submission.getAttachments(),
                submission.getAnswers(),
                submission.getExamResultId(),
                assignment.isShowScore() ? submission.getScore() : null,
                assignment.isShowPassFail() ? submission.getPassed() : null,
                assignment.isShowGrade() ? submission.getGradeValue() : null,
                submission.getFeedback(),
                submission.getSubmittedAt(),
                submission.getGradedAt(),
                submission.getStatus(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }

    private ExamAssignmentResponse toExamResponse(ExamService.ExamAssignmentData exam) {
        return new ExamAssignmentResponse(
                exam.id(),
                exam.name(),
                exam.category(),
                exam.gradingType(),
                exam.gradeScale(),
                exam.totalQuestions(),
                exam.totalPossibleScore(),
                exam.passScore()
        );
    }

    private List<AssignmentQuestionResultResponse> existingQuestionResults(
            Assignment assignment, AssignmentSubmission submission) {
        if (submission == null || submission.getExamResultId() == null || !assignment.isShowQuestionResults()) {
            return List.of();
        }
        return examService
                .getQuestionResultsForAssignment(TenantContextHolder.getRequiredTenant(), submission.getExamResultId())
                .stream()
                .map(question -> new AssignmentQuestionResultResponse(
                        question.questionNumber(),
                        question.selectedChoice(),
                        question.correct(),
                        question.questionType(),
                        assignment.isShowScore() ? question.earnedScore() : null,
                        assignment.isShowCorrectAnswers() ? question.correctAnswer() : null
                ))
                .toList();
    }
}
