package com.lumie.assignment.application.service;

import com.lumie.common.auth.CurrentStudentResolver;
import com.lumie.assignment.application.dto.request.CreateAssignmentRequest;
import com.lumie.assignment.application.dto.request.CreateSubmissionRequest;
import com.lumie.assignment.application.dto.request.SubmitAssignmentRequest;
import com.lumie.assignment.application.dto.request.UpdateAssignmentRequest;
import com.lumie.assignment.application.dto.request.UpdateSubmissionRequest;
import com.lumie.assignment.application.dto.response.AssignmentQuestionResultResponse;
import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.dto.response.AssignmentSubmissionResultResponse;
import com.lumie.assignment.application.dto.response.SubmissionResponse;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.entity.AssignmentTarget;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.repository.AssignmentTargetRepository;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentTargetType;
import com.lumie.assignment.domain.vo.EvaluationMode;
import com.lumie.assignment.domain.vo.SubmissionMode;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.api.ExamService;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentCommandService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentTargetRepository targetRepository;
    private final AssignmentRecipientService recipientService;
    private final ClassService classService;
    private final StudentService studentService;
    private final ExamService examService;
    private final AssignmentLifecycleService lifecycleService;

    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        String slug = TenantContextHolder.getRequiredTenant();
        if (request.dueDate() == null) {
            throw new BusinessException(AssignmentErrorCode.DUE_DATE_REQUIRED);
        }
        SubmissionMode submissionMode = parseSubmissionMode(request.submissionMode());
        EvaluationMode evaluationMode = normalizeEvaluationMode(submissionMode, request.evaluationMode());
        AssignmentStatus status = request.status() != null
                ? parseAssignmentStatus(request.status())
                : AssignmentStatus.ACTIVE;
        List<Long> targetClassIds = normalizeClassTargets(request.classId(), request.targetClassIds());
        List<Long> targetStudentIds = normalizeIds(request.targetStudentIds());
        validateTargets(slug, targetClassIds, targetStudentIds);

        Long linkedExamId = createLinkedExam(slug, request);
        requireExamManualWorkflow(submissionMode, evaluationMode, linkedExamId);

        Assignment entity = Assignment.create(
            legacyClassId(slug, targetClassIds, targetStudentIds),
            request.dueDate(),
            request.maxScore(),
            request.attachments(),
            status,
            submissionMode,
            evaluationMode,
            linkedExamId,
            true,
            true,
            false,
            true,
            defaultBoolean(request.showCorrectAnswers(), false)
        );
        entity.closeIfOverdue(Instant.now());

        Assignment saved = assignmentRepository.save(entity);
        List<AssignmentTarget> targets = replaceTargets(saved.getId(), targetClassIds, targetStudentIds);
        recipientService.snapshotRecipients(slug, saved.getId(), targetClassIds, targetStudentIds);

        log.info("Assignment created: {} targets(classes={}, students={})",
                saved.getId(), targetClassIds.size(), targetStudentIds.size());
        return AssignmentResponse.from(saved, targets, examName(slug, saved.getLinkedExamId()));
    }

    public AssignmentResponse updateAssignment(Long id, UpdateAssignmentRequest request) {
        Assignment entity = assignmentRepository.findById(id)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                "Assignment not found with id: %d".formatted(id)));

        String slug = TenantContextHolder.getRequiredTenant();
        List<AssignmentTarget> existingTargets = targetRepository.findByAssignmentId(id);
        boolean targetUpdateRequested = request.targetClassIds() != null || request.targetStudentIds() != null;

        List<Long> targetClassIds = request.targetClassIds() != null
                ? normalizeIds(request.targetClassIds())
                : targetIds(existingTargets, AssignmentTargetType.CLASS);
        List<Long> targetStudentIds = request.targetStudentIds() != null
                ? normalizeIds(request.targetStudentIds())
                : targetIds(existingTargets, AssignmentTargetType.STUDENT);
        validateTargets(slug, targetClassIds, targetStudentIds);

        SubmissionMode submissionMode = request.submissionMode() != null
                ? parseSubmissionMode(request.submissionMode())
                : entity.getSubmissionMode();
        EvaluationMode evaluationMode = request.evaluationMode() != null
                ? normalizeEvaluationMode(submissionMode, request.evaluationMode())
                : normalizeEvaluationMode(submissionMode, entity.getEvaluationMode().name());
        Long linkedExamId = entity.getLinkedExamId();
        if (submissionMode == SubmissionMode.EXAM_MANUAL) {
            validateLinkedExam(slug, linkedExamId);
        }
        requireExamManualWorkflow(submissionMode, evaluationMode, linkedExamId);

        var dueDate = request.dueDate() != null ? request.dueDate() : entity.getDueDate();
        Integer maxScore = request.maxScore() != null ? request.maxScore() : entity.getMaxScore();
        var attachments = request.attachments() != null ? request.attachments() : entity.getAttachments();
        Instant now = Instant.now();
        AssignmentStatus status = request.status() != null ? parseAssignmentStatus(request.status()) : entity.getStatus();
        if (request.dueDate() != null && request.dueDate().isAfter(now)) {
            status = AssignmentStatus.ACTIVE;
        }

        entity.updateInfo(dueDate, maxScore, attachments);
        entity.updateWorkflow(
                legacyClassId(slug, targetClassIds, targetStudentIds),
                submissionMode,
                evaluationMode,
                true,
                true,
                false,
                true,
                request.showCorrectAnswers() != null ? request.showCorrectAnswers() : entity.isShowCorrectAnswers(),
                status
        );
        entity.closeIfOverdue(now);

        Assignment saved = assignmentRepository.save(entity);
        List<AssignmentTarget> targets = targetUpdateRequested
                ? replaceTargets(saved.getId(), targetClassIds, targetStudentIds)
                : existingTargets;
        if (targetUpdateRequested) {
            recipientService.snapshotRecipients(slug, saved.getId(), targetClassIds, targetStudentIds);
        }

        log.info("Assignment updated: {}", saved.getId());
        return AssignmentResponse.from(saved, targets, examName(slug, saved.getLinkedExamId()));
    }

    public void deleteAssignment(Long id) {
        Assignment entity = assignmentRepository.findById(id)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                "Assignment not found with id: %d".formatted(id)));

        assignmentRepository.delete(entity);

        log.info("Assignment deleted: {}", id);
    }

    public SubmissionResponse createSubmission(Long assignmentId, CreateSubmissionRequest request) {
        lifecycleService.closeOverdueAssignment(assignmentId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                "Assignment not found with id: %d".formatted(assignmentId)));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_CLOSED);
        }

        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, request.studentId())) {
            throw new BusinessException(AssignmentErrorCode.DUPLICATE_SUBMISSION,
                "Student %d has already submitted for assignment %d".formatted(request.studentId(), assignmentId));
        }

        AssignmentSubmission submission = AssignmentSubmission.create(assignmentId, request.studentId());
        var result = examService.gradeManualAnswers(
                TenantContextHolder.getRequiredTenant(),
                assignment.getLinkedExamId(),
                request.studentId(),
                request.answers());
        submission.submitExam(request.answers(), result.examResultId(), result.totalScore(),
                result.passed(), result.grade() != null ? String.valueOf(result.grade()) : null);

        AssignmentSubmission saved = submissionRepository.save(submission);

        log.info("Submission created: {} for assignment: {}", saved.getId(), assignmentId);
        return SubmissionResponse.from(saved);
    }

    public AssignmentSubmissionResultResponse submitCurrentStudent(Long assignmentId, SubmitAssignmentRequest request) {
        lifecycleService.closeOverdueAssignment(assignmentId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                        "Assignment not found with id: %d".formatted(assignmentId)));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_CLOSED);
        }

        Long studentId = currentStudentId();
        if (!isTargetedToStudent(assignmentId, studentId)) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_VISIBLE);
        }

        if (submissionRepository.findFirstByAssignmentIdAndStudentId(assignmentId, studentId).isPresent()) {
            throw new BusinessException(AssignmentErrorCode.DUPLICATE_SUBMISSION,
                    "Student %d has already submitted for assignment %d".formatted(studentId, assignmentId));
        }
        AssignmentSubmission submission = AssignmentSubmission.create(assignmentId, studentId);

        List<AssignmentQuestionResultResponse> questionResults = List.of();
        if (request.answers() == null || request.answers().isEmpty()) {
            throw new BusinessException(AssignmentErrorCode.INVALID_SUBMISSION, "Answers are required");
        }
        ExamService.ManualGradeResult result = examService.gradeManualAnswers(
                TenantContextHolder.getRequiredTenant(),
                assignment.getLinkedExamId(),
                studentId,
                request.answers());
        submission.submitExam(request.answers(), result.examResultId(), result.totalScore(),
                result.passed(), result.grade() != null ? String.valueOf(result.grade()) : null);
        questionResults = filteredQuestionResults(assignment, result.questionResults());

        AssignmentSubmission saved = submissionRepository.save(submission);
        return new AssignmentSubmissionResultResponse(
                studentSubmissionResponse(assignment, saved),
                assignment.isShowScore(),
                assignment.isShowPassFail(),
                assignment.isShowGrade(),
                assignment.isShowQuestionResults(),
                assignment.isShowCorrectAnswers(),
                questionResults
        );
    }

    public SubmissionResponse updateSubmission(Long assignmentId, Long submissionId,
                                               UpdateSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                        "Assignment not found with id: %d".formatted(assignmentId)));
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.SUBMISSION_NOT_FOUND,
                "Submission not found with id: %d".formatted(submissionId)));

        if (!submission.getAssignmentId().equals(assignmentId)) {
            throw new BusinessException(AssignmentErrorCode.SUBMISSION_NOT_FOUND,
                "Submission not found with id: %d".formatted(submissionId));
        }
        if (assignment.getEvaluationMode() == EvaluationMode.EXAM_AUTO) {
            throw new BusinessException(AssignmentErrorCode.INVALID_SUBMISSION,
                    "Exam-linked assignments are graded automatically");
        }

        if (request.status() != null && parseSubmissionStatus(request.status()) == SubmissionStatus.RETURNED) {
            submission.returnToStudent(request.feedback());
        } else {
            gradeTextSubmission(assignment.getEvaluationMode(), submission, request);
        }

        AssignmentSubmission saved = submissionRepository.save(submission);

        log.info("Submission {} updated for assignment: {}", submissionId, assignmentId);
        return SubmissionResponse.from(saved);
    }

    private void gradeTextSubmission(EvaluationMode evaluationMode, AssignmentSubmission submission,
                                     UpdateSubmissionRequest request) {
        switch (evaluationMode) {
            case PASS_FAIL -> {
                if (request.passed() == null) {
                    throw new BusinessException(AssignmentErrorCode.INVALID_SUBMISSION, "Pass/fail result is required");
                }
                submission.gradePassFail(request.passed(), request.feedback());
            }
            case GRADE -> {
                if (request.gradeValue() == null || request.gradeValue().isBlank()) {
                    throw new BusinessException(AssignmentErrorCode.INVALID_SUBMISSION, "Grade value is required");
                }
                submission.gradeValue(request.gradeValue(), request.feedback());
            }
            case SCORE -> {
                if (request.score() == null) {
                    throw new BusinessException(AssignmentErrorCode.INVALID_SUBMISSION, "Score is required");
                }
                submission.grade(request.score(), request.feedback());
            }
            case EXAM_AUTO -> throw new BusinessException(AssignmentErrorCode.INVALID_SUBMISSION,
                    "Exam-linked assignments are graded automatically");
        }
    }

    private List<AssignmentTarget> replaceTargets(Long assignmentId, List<Long> classIds, List<Long> studentIds) {
        targetRepository.deleteByAssignmentId(assignmentId);
        List<AssignmentTarget> targets = new ArrayList<>();
        classIds.forEach(classId -> targets.add(AssignmentTarget.create(assignmentId, AssignmentTargetType.CLASS, classId)));
        studentIds.forEach(studentId -> targets.add(AssignmentTarget.create(assignmentId, AssignmentTargetType.STUDENT, studentId)));
        return targetRepository.saveAll(targets);
    }

    private void validateTargets(String slug, List<Long> classIds, List<Long> studentIds) {
        if (classIds.isEmpty() && studentIds.isEmpty()) {
            throw new BusinessException(AssignmentErrorCode.TARGET_REQUIRED);
        }
        for (Long classId : classIds) {
            var ignored = classService.getClass(slug, classId)
                    .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CLASS_NOT_FOUND,
                            "Class not found with id: " + classId));
        }
        if (!studentIds.isEmpty()) {
            var students = studentService.getStudentsByIds(slug, studentIds).students();
            Set<Long> foundIds = students.stream()
                    .map(StudentService.StudentData::id)
                    .collect(java.util.stream.Collectors.toSet());
            for (Long studentId : studentIds) {
                if (!foundIds.contains(studentId)) {
                    throw new BusinessException(AssignmentErrorCode.STUDENT_NOT_FOUND,
                            "Student not found with id: " + studentId);
                }
            }
        }
    }

    private void validateLinkedExam(String slug, Long linkedExamId) {
        if (linkedExamId == null || examService.getExamForAssignment(slug, linkedExamId).isEmpty()) {
            throw new BusinessException(AssignmentErrorCode.EXAM_NOT_FOUND);
        }
    }

    private String examName(String slug, Long linkedExamId) {
        if (linkedExamId == null) {
            return null;
        }
        return examService.getExamForAssignment(slug, linkedExamId)
                .map(ExamService.ExamAssignmentData::name)
                .orElse(null);
    }

    private Long createLinkedExam(String slug, CreateAssignmentRequest request) {
        if (request.exam() == null) {
            throw new BusinessException(AssignmentErrorCode.EXAM_NOT_FOUND, "New exam data is required");
        }
        return examService.createExamForAssignment(slug, new ExamService.CreateExamData(
                request.exam().name(),
                request.exam().category(),
                request.exam().gradingType(),
                request.exam().gradeScale(),
                request.exam().totalQuestions(),
                request.exam().correctAnswers(),
                request.exam().questionScores(),
                request.exam().questionTypes(),
                request.exam().passScore()
        ));
    }

    private boolean isTargetedToStudent(Long assignmentId, Long studentId) {
        return recipientService.isRecipient(assignmentId, studentId);
    }

    private Long currentStudentId() {
        return CurrentStudentResolver.requireStudentId(studentService, AssignmentErrorCode.STUDENT_NOT_FOUND);
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

    private List<AssignmentQuestionResultResponse> filteredQuestionResults(
            Assignment assignment, List<ExamService.QuestionGradeData> questionResults) {
        if (!assignment.isShowQuestionResults()) {
            return List.of();
        }
        return questionResults.stream()
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

    private List<Long> normalizeClassTargets(Long legacyClassId, List<Long> classIds) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (legacyClassId != null) {
            ids.add(legacyClassId);
        }
        ids.addAll(normalizeIds(classIds));
        return ids.stream().toList();
    }

    private List<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private List<Long> targetIds(List<AssignmentTarget> targets, AssignmentTargetType type) {
        return targets.stream()
                .filter(target -> target.getTargetType() == type)
                .map(AssignmentTarget::getTargetId)
                .distinct()
                .toList();
    }

    private Long legacyClassId(String slug, List<Long> classIds, List<Long> studentIds) {
        return classIds.stream()
                .findFirst()
                .orElseGet(() -> classService.getEnrolledClassesForStudents(slug, studentIds).stream()
                        .findFirst()
                        .map(ClassService.EnrolledClassData::classId)
                        .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CLASS_NOT_FOUND,
                                "Individual assignment targets must have at least one enrolled class")));
    }

    private Boolean defaultBoolean(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    private SubmissionMode parseSubmissionMode(String mode) {
        if (mode == null) {
            return SubmissionMode.EXAM_MANUAL;
        }
        try {
            return SubmissionMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS, "Invalid submission mode: " + mode);
        }
    }

    private EvaluationMode normalizeEvaluationMode(SubmissionMode submissionMode, String mode) {
        EvaluationMode evaluationMode;
        try {
            evaluationMode = mode == null
                    ? (submissionMode == SubmissionMode.EXAM_MANUAL ? EvaluationMode.EXAM_AUTO : EvaluationMode.SCORE)
                    : EvaluationMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS, "Invalid evaluation mode: " + mode);
        }
        if (submissionMode == SubmissionMode.EXAM_MANUAL && evaluationMode != EvaluationMode.EXAM_AUTO) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS,
                    "EXAM_MANUAL assignments must use EXAM_AUTO evaluation");
        }
        return evaluationMode;
    }

    private void requireExamManualWorkflow(SubmissionMode submissionMode, EvaluationMode evaluationMode, Long linkedExamId) {
        if (submissionMode != SubmissionMode.EXAM_MANUAL || evaluationMode != EvaluationMode.EXAM_AUTO || linkedExamId == null) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS,
                    "Assignments must be linked exam manual grading assignments");
        }
    }

    private AssignmentStatus parseAssignmentStatus(String status) {
        try {
            return AssignmentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS, "Invalid assignment status: " + status);
        }
    }

    private SubmissionStatus parseSubmissionStatus(String status) {
        try {
            return SubmissionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS, "Invalid submission status: " + status);
        }
    }
}
