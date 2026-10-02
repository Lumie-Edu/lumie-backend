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
import com.lumie.assignment.application.port.out.AssignmentAudienceReadPort;
import com.lumie.assignment.application.port.out.AssignmentAudienceReadPort.Targets;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.entity.AssignmentRecipient;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentRecipientRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentStatus;
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
    private final AssignmentAudienceReadPort audienceReadPort;
    private final AssignmentRecipientRepository recipientRepository;
    private final AssignmentRecipientService recipientService;
    private final ClassService classService;
    private final StudentService studentService;
    private final ExamService examService;
    private final AssignmentLifecycleService lifecycleService;

    public AssignmentResponse getAssignment(Long id) {
        lifecycleService.closeOverdueAssignment(id);
        Assignment assignment = assignmentRepository.findById(id)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                "Assignment not found with id: %d".formatted(id)));
        return toAssignmentResponse(assignment, audienceReadPort.findByAssignmentId(id));
    }

    public Page<AssignmentResponse> listAssignments(Long classId, AssignmentStatus status,
                                                     Pageable pageable) {
        Instant now = Instant.now();
        AssignmentSearchCriteria criteria = new AssignmentSearchCriteria(classId, status, now);
        Page<Assignment> page = assignmentRepository.findAll(criteria, pageable);
        Map<Long, Targets> targetsByAssignment = targetsByAssignment(page.getContent());
        Set<Long> unverifiedIds = unverifiedRosterIds(page.getContent());
        return page.map(assignment -> assignmentResponse(
                assignment,
                targetsByAssignment.getOrDefault(assignment.getId(), emptyTargets()),
                examName(assignment),
                now,
                rosterAccuracy(assignment.getId(), unverifiedIds)));
    }

    public Page<AssignmentResponse> getAssignmentsByClass(Long classId, Pageable pageable) {
        Page<Assignment> page = assignmentRepository.findByTargetClassId(classId, pageable);
        Map<Long, Targets> targetsByAssignment = targetsByAssignment(page.getContent());
        Set<Long> unverifiedIds = unverifiedRosterIds(page.getContent());
        Instant now = Instant.now();
        return page.map(assignment -> assignmentResponse(
                assignment,
                targetsByAssignment.getOrDefault(assignment.getId(), emptyTargets()),
                examName(assignment),
                now,
                rosterAccuracy(assignment.getId(), unverifiedIds)));
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
        List<AssignmentRecipient> recipients = recipientRepository.findByAssignmentId(assignmentId);
        Set<Long> studentIds = recipients.stream()
                .map(AssignmentRecipient::getStudentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<AssignmentSubmission> submissions = submissionRepository.findAllByAssignmentId(assignmentId);
        Map<Long, AssignmentSubmission> submissionsByRecipient = submissions.stream()
                .filter(submission -> submission.getRecipientId() != null)
                .collect(Collectors.toMap(AssignmentSubmission::getRecipientId,
                        submission -> submission, (left, right) -> right));
        Map<Long, AssignmentSubmission> legacySubmissionsByStudent = submissions.stream()
                .filter(submission -> submission.getRecipientId() == null)
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
        return recipients.stream()
                .map(recipient -> {
                    Long studentId = recipient.getStudentId();
                    AssignmentSubmission submission = submissionsByRecipient.get(recipient.getId());
                    if (submission == null && studentId != null) {
                        submission = legacySubmissionsByStudent.get(studentId);
                    }
                    var result = submission == null || submission.getExamResultId() == null
                            ? null : results.get(submission.getExamResultId());
                    return new AssignmentSubmissionStatusResponse(
                            recipient.getId(),
                            studentId,
                            studentId != null && studentNamesById.containsKey(studentId)
                                    ? studentNamesById.get(studentId) : "삭제된 학생",
                            submission != null ? submission.getStatus() : SubmissionStatus.PENDING,
                            submission == null ? null : result == null
                                    || !java.util.Objects.equals(submission.getStudentId(), result.studentId())
                                    ? SubmissionResponse.from(submission) : SubmissionResponse.from(submission, result));
                })
                .toList();
    }

    public Page<AssignmentStudentGradeResponse> getStudentGrades(
            Long assignmentId, Long classId, Boolean hasActiveEnrollment, Boolean isActive,
            Boolean submitted, Pageable pageable) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
        String slug = TenantContextHolder.getRequiredTenant();
        List<AssignmentRecipient> recipients = recipientRepository.findByAssignmentId(assignmentId);
        if (recipients.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Long> studentIds = recipients.stream().map(AssignmentRecipient::getStudentId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        var students = (studentIds.isEmpty() ? List.<StudentService.StudentData>of()
                : studentService.getStudentsByIds(slug, studentIds).students()).stream()
                .collect(Collectors.toMap(StudentService.StudentData::id, student -> student));
        var enrolledClasses = studentIds.isEmpty()
                ? List.<ClassService.EnrolledClassData>of()
                : classService.getEnrolledClassesForStudents(slug, studentIds);
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
        List<AssignmentSubmission> submissions = submissionRepository.findAllByAssignmentId(assignmentId);
        Map<Long, AssignmentSubmission> submissionsByRecipient = submissions.stream()
                .filter(submission -> submission.getRecipientId() != null)
                .collect(Collectors.toMap(AssignmentSubmission::getRecipientId,
                        submission -> submission, (left, right) -> right));
        Map<Long, AssignmentSubmission> legacyByStudent = submissions.stream()
                .filter(submission -> submission.getRecipientId() == null && submission.getStudentId() != null)
                .collect(Collectors.toMap(AssignmentSubmission::getStudentId,
                        submission -> submission, (left, right) -> right));
        List<AssignmentStudentGradeResponse> rows = recipients.stream()
                .filter(recipient -> classId == null || classRecipientIds.contains(recipient.getStudentId()))
                .filter(recipient -> hasActiveEnrollment == null
                        || activeEnrollmentIds.contains(recipient.getStudentId()) == hasActiveEnrollment)
                .filter(recipient -> isActive == null
                        || (students.get(recipient.getStudentId()) != null
                            && students.get(recipient.getStudentId()).isActive() == isActive))
                .map(recipient -> {
                    AssignmentSubmission submission = submissionsByRecipient.get(recipient.getId());
                    if (submission == null && recipient.getStudentId() != null) {
                        submission = legacyByStudent.get(recipient.getStudentId());
                    }
                    return toAssignmentStudentGrade(recipient, students.get(recipient.getStudentId()),
                            submission == null ? null : gradesByStudent.get(recipient.getStudentId()), submission);
                })
                .filter(row -> submitted == null || (row.submissionStatus() != SubmissionStatus.PENDING) == submitted)
                .sorted(assignmentGradeComparator(pageable.getSort()))
                .toList();
        return InMemoryPage.of(rows, pageable);
    }

    private AssignmentStudentGradeResponse toAssignmentStudentGrade(
            AssignmentRecipient recipient, StudentService.StudentData student,
            ExamService.StudentGradeData grade, AssignmentSubmission submission) {
        if (student == null) {
            return new AssignmentStudentGradeResponse(recipient.getId(), null, null, false,
                    "삭제된 학생", submission == null ? null : submission.getScore(), null, null, null,
                    submission != null && submission.getPassed() != null ? "PASS_FAIL" : null,
                    submission == null ? null : submission.getPassed(),
                    submission == null ? null : submission.getSubmittedAt(),
                    submission == null ? null : submission.getExamResultId(),
                    submission == null ? SubmissionStatus.PENDING : submission.getStatus());
        }
        if (submission == null) {
            return new AssignmentStudentGradeResponse(recipient.getId(), student.id(), student.studentPhone(),
                    true, student.studentName(),
                    null, null, null, null, null, null, null, null, SubmissionStatus.PENDING);
        }
        if (grade == null) {
            return new AssignmentStudentGradeResponse(recipient.getId(), student.id(), student.studentPhone(),
                    true, student.studentName(), submission.getScore(), null, null, null,
                    submission.getPassed() != null ? "PASS_FAIL" : null,
                    submission.getPassed(), submission.getSubmittedAt(), submission.getExamResultId(),
                    submission.getStatus());
        }
        return new AssignmentStudentGradeResponse(recipient.getId(), grade.studentId(), grade.phoneNumber(), grade.isRegistered(),
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
        Set<Long> visibleAssignmentIds = new LinkedHashSet<>(recipientService.findEligibleAssignmentIds(studentId));
        visibleAssignmentIds.addAll(submissionRepository.findAssignmentIdsByStudentId(studentId));
        if (visibleAssignmentIds.isEmpty()) {
            return List.of();
        }

        List<Assignment> assignments = assignmentRepository
                .findByIdIn(List.copyOf(visibleAssignmentIds))
                .stream()
                .filter(assignment -> assignment.getSubmissionMode() == SubmissionMode.EXAM_MANUAL)
                .sorted(Comparator
                        .comparing(Assignment::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Assignment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        Set<Long> unverifiedIds = unverifiedRosterIds(assignments);
        Instant now = Instant.now();
        return assignments.stream()
                .map(assignment -> studentAssignmentResponse(assignment, examName(assignment), now,
                        rosterAccuracy(assignment.getId(), unverifiedIds)))
                .toList();
    }

    public StudentAssignmentDetailResponse getCurrentStudentAssignment(Long assignmentId) {
        lifecycleService.closeOverdueAssignment(assignmentId);
        Long studentId = currentStudentId();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                        "Assignment not found with id: %d".formatted(assignmentId)));
        AssignmentSubmission submission = submissionRepository
                .findFirstByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElse(null);
        boolean eligible = recipientService.canSubmit(assignmentId, studentId);
        if (!eligible && submission == null) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_VISIBLE);
        }
        ExamAssignmentResponse exam = eligible && assignment.getSubmissionMode() == SubmissionMode.EXAM_MANUAL
                ? examService.getExamForAssignment(TenantContextHolder.getRequiredTenant(), assignment.getLinkedExamId())
                        .map(this::toExamResponse)
                        .orElse(null)
                : null;

        return new StudentAssignmentDetailResponse(
                studentAssignmentResponse(assignment, exam != null ? exam.name() : examName(assignment)),
                effectiveStatus(assignment, Instant.now()) == AssignmentStatus.ACTIVE && eligible,
                submission != null ? studentSubmissionResponse(assignment, submission) : null,
                exam,
                existingQuestionResults(assignment, submission)
        );
    }

    private AssignmentResponse toAssignmentResponse(Assignment assignment, Targets targets) {
        return assignmentResponse(assignment, targets, examName(assignment), Instant.now(),
                recipientService.rosterAccuracy(assignment.getId()));
    }

    private AssignmentResponse assignmentResponse(
            Assignment assignment, Targets targets, String examName, Instant now, String rosterAccuracy) {
        return new AssignmentResponse(
                assignment.getId(),
                targets.firstClassId(),
                assignment.getDueDate(),
                assignment.getMaxScore(),
                assignment.getAttachments(),
                effectiveStatus(assignment, now),
                assignment.getSubmissionMode(),
                assignment.getEvaluationMode(),
                assignment.getLinkedExamId(),
                examName,
                targets.classIds(),
                targets.studentIds(),
                assignment.isShowScore(),
                assignment.isShowPassFail(),
                assignment.isShowGrade(),
                assignment.isShowQuestionResults(),
                assignment.isShowCorrectAnswers(),
                rosterAccuracy,
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    private AssignmentResponse studentAssignmentResponse(Assignment assignment, String examName) {
        return studentAssignmentResponse(assignment, examName, Instant.now(),
                recipientService.rosterAccuracy(assignment.getId()));
    }

    private AssignmentResponse studentAssignmentResponse(Assignment assignment, String examName,
                                                         Instant now, String rosterAccuracy) {
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
                rosterAccuracy,
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

    private Map<Long, Targets> targetsByAssignment(List<Assignment> assignments) {
        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .toList();
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        return audienceReadPort.findByAssignmentIds(assignmentIds);
    }

    private Set<Long> unverifiedRosterIds(List<Assignment> assignments) {
        List<Long> ids = assignments.stream().map(Assignment::getId).toList();
        if (ids.isEmpty()) {
            return Set.of();
        }
        return recipientRepository.findByAssignmentIdIn(ids).stream()
                .filter(recipient -> !"EXACT".equals(recipient.getSourceProvenance()))
                .map(recipient -> recipient.getAssignmentId())
                .collect(Collectors.toSet());
    }

    private static String rosterAccuracy(Long assignmentId, Set<Long> unverifiedIds) {
        return unverifiedIds.contains(assignmentId) ? "UNVERIFIED" : "EXACT";
    }

    private static Targets emptyTargets() {
        return new Targets(List.of(), List.of());
    }

    private SubmissionResponse studentSubmissionResponse(Assignment assignment, AssignmentSubmission submission) {
        var result = submission.getExamResultId() == null ? null
                : currentResults(assignment.getId()).get(submission.getExamResultId());
        var current = result == null || !java.util.Objects.equals(submission.getStudentId(), result.studentId())
                ? SubmissionResponse.from(submission)
                : SubmissionResponse.from(submission, result);
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
