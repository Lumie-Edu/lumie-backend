package com.lumie.assignment.application.service;

import com.lumie.common.api.InMemoryPage;
import com.lumie.common.auth.CurrentStudentResolver;
import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.dto.response.AssignmentQuestionResultResponse;
import com.lumie.assignment.application.dto.response.AssignmentSubmissionStatusResponse;
import com.lumie.assignment.application.dto.response.AssignmentStudentGradeResponse;
import com.lumie.assignment.application.dto.response.ExamAssignmentResponse;
import com.lumie.assignment.application.dto.response.StudentAssignmentDetailResponse;
import com.lumie.assignment.application.dto.response.SubmissionResponse;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.entity.AssignmentTarget;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentRecipientRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.repository.AssignmentTargetRepository;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentTargetType;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import com.lumie.classroom.api.ClassService;
import com.lumie.assignment.domain.vo.SubmissionMode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.api.ExamService;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final AssignmentRecipientRepository recipientRepository;
    private final ClassService classService;
    private final StudentService studentService;
    private final ExamService examService;
    private final AssignmentLifecycleService lifecycleService;

    public AssignmentResponse getAssignment(Long id) {
        lifecycleService.closeOverdueAssignment(id);
        Assignment assignment = assignmentRepository.findById(id)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                "Assignment not found with id: %d".formatted(id)));
        return toAssignmentResponse(assignment, targetRepository.findByAssignmentId(id));
    }

    public Page<AssignmentResponse> listAssignments(Long classId, AssignmentStatus status,
                                                     Pageable pageable) {
        Instant now = Instant.now();
        AssignmentSearchCriteria criteria = new AssignmentSearchCriteria(classId, status, now);
        Page<Assignment> page = assignmentRepository.findAll(criteria, pageable);
        Map<Long, List<AssignmentTarget>> targetsByAssignment = targetsByAssignment(page.getContent());
        return page.map(assignment -> assignmentResponse(
                assignment,
                targetsByAssignment.getOrDefault(assignment.getId(), List.of()),
                examName(assignment),
                now));
    }

    public Page<AssignmentResponse> getAssignmentsByClass(Long classId, Pageable pageable) {
        Page<Assignment> page = assignmentRepository.findByTargetClassId(classId, pageable);
        Map<Long, List<AssignmentTarget>> targetsByAssignment = targetsByAssignment(page.getContent());
        Instant now = Instant.now();
        return page.map(assignment -> assignmentResponse(
                assignment,
                targetsByAssignment.getOrDefault(assignment.getId(), List.of()),
                examName(assignment),
                now));
    }

    public Page<SubmissionResponse> getSubmissions(Long assignmentId, Pageable pageable) {
        lifecycleService.closeOverdueAssignment(assignmentId);
        var results = currentResults(assignmentId);
        return submissionRepository.findByAssignmentId(assignmentId, pageable)
            .map(submission -> SubmissionResponse.from(submission, results.get(submission.getExamResultId())));
    }

    public List<AssignmentSubmissionStatusResponse> getSubmissionStatuses(Long assignmentId) {
        lifecycleService.closeOverdueAssignment(assignmentId);
        var ignored = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                        "Assignment not found with id: %d".formatted(assignmentId)));

        String slug = TenantContextHolder.getRequiredTenant();
        Set<Long> studentIds = new LinkedHashSet<>(recipientRepository.findStudentIdsByAssignmentId(assignmentId));

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

        var results = currentResults(assignmentId);
        return studentIds.stream()
                .map(studentId -> {
                    AssignmentSubmission submission = submissionsByStudent.get(studentId);
                    return new AssignmentSubmissionStatusResponse(
                            studentId,
                            studentNamesById.get(studentId),
                            submission != null ? submission.getStatus() : SubmissionStatus.PENDING,
                            submission != null ? SubmissionResponse.from(submission, results.get(submission.getExamResultId())) : null);
                })
                .toList();
    }

    public Page<AssignmentStudentGradeResponse> getStudentGrades(
            Long assignmentId, Long classId, Boolean hasActiveEnrollment, Boolean isActive,
            Boolean submitted, Pageable pageable) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
        String slug = TenantContextHolder.getRequiredTenant();
        List<Long> recipientIds = recipientRepository.findStudentIdsByAssignmentId(assignmentId);
        if (recipientIds.isEmpty()) {
            return Page.empty(pageable);
        }
        var students = studentService.getStudentsByIds(slug, recipientIds).students().stream()
                .collect(Collectors.toMap(StudentService.StudentData::id, student -> student));
        var enrolledClasses = classService.getEnrolledClassesForStudents(slug, recipientIds);
        Set<Long> activeEnrollmentIds = enrolledClasses.stream()
                .map(ClassService.EnrolledClassData::studentId)
                .collect(Collectors.toSet());
        Set<Long> classRecipientIds = classId == null ? Set.of() : enrolledClasses.stream()
                .filter(enrollment -> enrollment.classId().equals(classId))
                .map(ClassService.EnrolledClassData::studentId)
                .collect(Collectors.toSet());
        var gradesByStudent = examService.getStudentGradesForAssignment(slug, assignment.getLinkedExamId()).stream()
                .filter(grade -> grade.studentId() != null)
                .collect(Collectors.toMap(ExamService.StudentGradeData::studentId, grade -> grade, (left, right) -> left));
        List<AssignmentStudentGradeResponse> rows = recipientIds.stream()
                .filter(studentId -> classId == null || classRecipientIds.contains(studentId))
                .filter(studentId -> hasActiveEnrollment == null || activeEnrollmentIds.contains(studentId) == hasActiveEnrollment)
                .filter(studentId -> isActive == null || (students.get(studentId) != null && students.get(studentId).isActive() == isActive))
                .map(studentId -> toAssignmentStudentGrade(students.get(studentId), gradesByStudent.get(studentId)))
                .filter(row -> submitted == null || (row.submissionStatus() != SubmissionStatus.PENDING) == submitted)
                .sorted(assignmentGradeComparator(pageable.getSort()))
                .toList();
        return InMemoryPage.of(rows, pageable);
    }

    private AssignmentStudentGradeResponse toAssignmentStudentGrade(
            StudentService.StudentData student, ExamService.StudentGradeData grade) {
        if (student == null) {
            throw new BusinessException(AssignmentErrorCode.STUDENT_NOT_FOUND);
        }
        if (grade == null) {
            return new AssignmentStudentGradeResponse(student.id(), student.studentPhone(), true, student.studentName(),
                    null, null, null, null, null, null, null, null, SubmissionStatus.PENDING);
        }
        return new AssignmentStudentGradeResponse(grade.studentId(), grade.phoneNumber(), grade.isRegistered(),
                grade.studentName(), grade.score(), grade.rank(), grade.percentile(), grade.grade(),
                grade.examCategory(), grade.isPassed(), Instant.parse(grade.submittedAt()), grade.resultId(),
                SubmissionStatus.GRADED);
    }

    private Comparator<AssignmentStudentGradeResponse> assignmentGradeComparator(Sort sort) {
        Sort.Order order = sort.stream().findFirst().orElse(Sort.Order.asc("rank"));
        Comparator<AssignmentStudentGradeResponse> comparator = switch (order.getProperty()) {
            case "totalScore" -> Comparator.comparing(AssignmentStudentGradeResponse::score,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "submittedAt" -> Comparator.comparing(AssignmentStudentGradeResponse::submittedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(AssignmentStudentGradeResponse::rank,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if (order.isDescending()) {
            comparator = comparator.reversed();
        }
        return Comparator.comparing((AssignmentStudentGradeResponse row) -> row.submissionStatus() == SubmissionStatus.PENDING)
                .thenComparing(comparator)
                .thenComparing(AssignmentStudentGradeResponse::studentName, Comparator.nullsLast(String::compareTo));
    }

    public Page<SubmissionResponse> getStudentSubmissions(Long assignmentId, Long studentId, Pageable pageable) {
        lifecycleService.closeOverdueAssignment(assignmentId);
        var results = currentResults(assignmentId);
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId, pageable)
            .map(submission -> SubmissionResponse.from(submission, results.get(submission.getExamResultId())));
    }

    public List<AssignmentResponse> getCurrentStudentAssignments() {
        Long studentId = currentStudentId();
        List<Long> visibleAssignmentIds = recipientRepository.findAssignmentIdsByStudentId(studentId);
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
        Instant now = Instant.now();
        return assignments.stream()
                .map(assignment -> studentAssignmentResponse(assignment, examName(assignment), now))
                .toList();
    }

    public StudentAssignmentDetailResponse getCurrentStudentAssignment(Long assignmentId) {
        lifecycleService.closeOverdueAssignment(assignmentId);
        Long studentId = currentStudentId();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                        "Assignment not found with id: %d".formatted(assignmentId)));
        if (!recipientRepository.existsByAssignmentIdAndStudentId(assignmentId, studentId)) {
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

    private AssignmentResponse toAssignmentResponse(Assignment assignment, List<AssignmentTarget> targets) {
        return assignmentResponse(assignment, targets, examName(assignment), Instant.now());
    }

    private AssignmentResponse assignmentResponse(
            Assignment assignment, List<AssignmentTarget> targets, String examName, Instant now) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getClassId(),
                assignment.getDueDate(),
                assignment.getMaxScore(),
                assignment.getAttachments(),
                effectiveStatus(assignment, now),
                assignment.getSubmissionMode(),
                assignment.getEvaluationMode(),
                assignment.getLinkedExamId(),
                examName,
                targets.stream()
                        .filter(target -> target.getTargetType() == AssignmentTargetType.CLASS)
                        .map(AssignmentTarget::getTargetId)
                        .toList(),
                targets.stream()
                        .filter(target -> target.getTargetType() == AssignmentTargetType.STUDENT)
                        .map(AssignmentTarget::getTargetId)
                        .toList(),
                assignment.isShowScore(),
                assignment.isShowPassFail(),
                assignment.isShowGrade(),
                assignment.isShowQuestionResults(),
                assignment.isShowCorrectAnswers(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    private AssignmentResponse studentAssignmentResponse(Assignment assignment, String examName) {
        return studentAssignmentResponse(assignment, examName, Instant.now());
    }

    private AssignmentResponse studentAssignmentResponse(Assignment assignment, String examName, Instant now) {
        return new AssignmentResponse(
                assignment.getId(),
                null,
                assignment.getDueDate(),
                assignment.getMaxScore(),
                assignment.getAttachments(),
                effectiveStatus(assignment, now),
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

    private AssignmentStatus effectiveStatus(Assignment assignment, Instant now) {
        if (assignment.getStatus() == AssignmentStatus.ACTIVE && !assignment.getDueDate().isAfter(now)) {
            return AssignmentStatus.CLOSED;
        }
        return assignment.getStatus();
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
        return CurrentStudentResolver.requireStudentId(studentService, AssignmentErrorCode.STUDENT_NOT_FOUND);
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
        var current = SubmissionResponse.from(submission,
                currentResults(assignment.getId()).get(submission.getExamResultId()));
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignmentId(),
                submission.getStudentId(),
                submission.getContent(),
                submission.getAttachments(),
                current.answers(),
                submission.getExamResultId(),
                assignment.isShowScore() ? current.score() : null,
                assignment.isShowPassFail() ? current.passed() : null,
                assignment.isShowGrade() ? current.gradeValue() : null,
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

    private Map<Long, ExamService.AssignmentResultData> currentResults(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
        return examService.getResultsForAssignment(TenantContextHolder.getRequiredTenant(), assignment.getLinkedExamId());
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
