package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.exam.application.dto.request.ApplyOmrSubmissionRequest;
import com.lumie.exam.application.dto.request.ConfirmOmrSubmissionReconciliationRequest;
import com.lumie.exam.application.dto.request.PreviewOmrSubmissionReconciliationRequest;
import com.lumie.exam.application.dto.request.RegisterStudentFromOmrSubmissionRequest;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.dto.response.OmrSubmissionResponse;
import com.lumie.exam.application.dto.response.OmrSubmissionReconciliationPreviewResponse;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.entity.OmrSubmissionQuestion;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.repository.OmrSubmissionRepository;
import com.lumie.exam.domain.vo.OmrGradingStatus;
import com.lumie.exam.domain.vo.OmrReconciliationStatus;
import com.lumie.student.api.StudentService;
import com.lumie.student.api.StudentRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OmrSubmissionService {

    private final OmrSubmissionRepository submissionRepository;
    private final ExamResultRepository resultRepository;
    private final OmrGradingJobRepository jobRepository;
    private final ResultCommandService resultCommandService;
    private final StudentService studentService;
    private final StudentRegistrationService studentRegistrationService;

    @Transactional
    public void createPendingSubmissions(Exam exam, Long jobId, List<String> imageKeys, Instant batchAcceptedAt) {
        List<OmrSubmission> submissions = java.util.stream.IntStream.range(0, imageKeys.size())
                .mapToObj(index -> OmrSubmission.pending(exam, jobId, index, imageKeys.get(index), batchAcceptedAt))
                .toList();
        submissionRepository.saveAll(submissions);
    }

    @Transactional(readOnly = true)
    public OmrSubmission getSubmission(Long jobId, int imageIndex) {
        return submissionRepository.findByJobIdAndImageIndex(jobId, imageIndex)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
    }

    @Transactional
    public boolean completeIfPending(Long jobId, int imageIndex, OmrGradingCallbackRequest request, String permanentImageKey) {
        OmrSubmission submission = getSubmissionForUpdate(jobId, imageIndex);
        if (submission.getGradingStatus() != OmrGradingStatus.PENDING) {
            return false;
        }
        List<OmrSubmissionQuestion> questions = request.results().stream()
                .map(q -> OmrSubmissionQuestion.create(q.questionNumber(), q.studentAnswer(), q.correctAnswer(),
                        q.score(), q.earnedScore(), q.questionType()))
                .toList();
        submission.complete(normalizeCallbackPhone(request.phoneNumber()), request.totalScore(),
                submission.getExam().isPassFail() ? null : request.grade(),
                permanentImageKey, questions);

        if (submission.getEffectivePhoneNumber() == null) {
            submission.markUnmatched("PHONE_NOT_DETECTED");
            return true;
        }

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        StudentService.StudentData student = studentService.getStudentByPhone(tenantSlug, submission.getEffectivePhoneNumber())
                .orElse(null);
        if (student == null) {
            submission.markUnmatched("NO_ACTIVE_STUDENT_MATCH");
            return true;
        }
        if (!student.isActive()) {
            submission.reject("INACTIVE_STUDENT_PHONE");
            return true;
        }

        submission.markMatched(student.id());
        applyAutomatic(submission, student);
        return true;
    }

    @Transactional
    public boolean failIfPending(Long jobId, int imageIndex, String reason) {
        OmrSubmission submission = getSubmissionForUpdate(jobId, imageIndex);
        if (submission.getGradingStatus() != OmrGradingStatus.PENDING) {
            return false;
        }
        submission.fail(reason);
        return true;
    }

    @Transactional
    public OmrSubmissionResponse apply(Long examId, Long submissionId, ApplyOmrSubmissionRequest request) {
        OmrSubmission submission = requireSubmission(examId, submissionId);
        requireCompletedSubmission(submission);
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        StudentService.StudentData student = studentService.getStudent(tenantSlug, request.studentId())
                .filter(StudentService.StudentData::isActive)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND));
        ExamResult existing = resultRepository.findByExamIdAndStudentId(examId, student.id()).orElse(null);
        requireAvailableForOfficialProjection(submission, existing);
        if (existing != null && !request.replaceCurrentResult()) {
            throw new BusinessException(ExamErrorCode.DUPLICATE_RESULT,
                    "이미 해당 학생의 시험 성적이 있습니다. 교체 여부를 확인하세요.");
        }
        submission.markMatched(student.id());
        saveOfficialProjection(
                submission,
                student,
                existing,
                true,
                "이미 다른 요청이 이 학생 또는 OMR submission의 공식 성적을 확정했습니다. 새로고침 후 다시 확인하세요.");
        return responseFor(submission, student.studentName());
    }

    @Transactional
    public OmrSubmissionResponse registerStudentAndApply(
            Long examId, Long submissionId, RegisterStudentFromOmrSubmissionRequest request) {
        OmrSubmission submission = requireSubmission(examId, submissionId);
        requireCompletedSubmission(submission);
        requireAvailableForOfficialProjection(submission, null);
        String phone = request.phone() == null || request.phone().isBlank()
                ? submission.getEffectivePhoneNumber()
                : PhoneNumberUtils.toDigits(request.phone());
        if (!PhoneNumberUtils.isValidStudentPhone(phone)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "전화번호 형식이 올바르지 않습니다. (예: 01012345678)");
        }
        StudentService.StudentData student = studentRegistrationService.registerStudent(
                TenantContextHolder.getRequiredTenant(),
                new StudentRegistrationService.RegisterStudentCommand(
                        request.name(), phone, request.studentHighschool(), request.studentBirthYear(),
                        request.parentPhone(), request.studentMemo()));
        submission.correctPhone(phone);
        submission.markMatched(student.id());
        saveOfficialProjection(
                submission,
                student,
                null,
                true,
                "이미 다른 요청이 이 학생 또는 OMR submission의 공식 성적을 확정했습니다. 새로고침 후 다시 확인하세요.");
        return responseFor(submission, student.studentName());
    }

    @Transactional
    public OmrSubmissionResponse correctPhone(Long examId, Long submissionId, String phoneNumber) {
        String normalized = PhoneNumberUtils.toDigits(phoneNumber);
        if (!PhoneNumberUtils.isValidStudentPhone(normalized)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "전화번호 형식이 올바르지 않습니다. (예: 01012345678)");
        }
        OmrSubmission submission = requireSubmission(examId, submissionId);
        requireCompletedSubmission(submission);
        requireAvailableForOfficialProjection(submission, null);
        submission.correctPhone(normalized);
        StudentService.StudentData student = studentService.getStudentByPhone(
                        TenantContextHolder.getRequiredTenant(), normalized)
                .orElse(null);
        if (student == null) {
            return responseFor(submission, null);
        }
        if (!student.isActive()) {
            submission.reject("INACTIVE_STUDENT_PHONE");
            return responseFor(submission, null);
        }
        submission.markMatched(student.id());
        return responseFor(submission, student.studentName());
    }

    @Transactional(readOnly = true)
    public OmrSubmissionReconciliationPreviewResponse previewReconciliation(
            Long examId, Long submissionId, PreviewOmrSubmissionReconciliationRequest request) {
        OmrSubmission submission = requireReconciliableSubmission(examId, submissionId);
        String phoneNumber = requireStudentPhone(request.phoneNumber());
        StudentService.StudentData student = studentService.getStudentByPhone(
                        TenantContextHolder.getRequiredTenant(), phoneNumber)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND,
                        "입력한 전화번호와 일치하는 학생이 없습니다."));
        if (!student.isActive()) {
            throw new BusinessException(ExamErrorCode.INACTIVE_STUDENT_PHONE);
        }
        ExamResult currentResult = resultRepository.findByExamIdAndStudentId(examId, student.id()).orElse(null);
        return OmrSubmissionReconciliationPreviewResponse.of(student, currentResult);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public OmrSubmissionResponse confirmReconciliation(
            Long examId, Long submissionId, ConfirmOmrSubmissionReconciliationRequest request) {
        OmrSubmission submission = requireReconciliableSubmission(examId, submissionId);
        String phoneNumber = requireStudentPhone(request.phoneNumber());
        StudentService.StudentData student = studentService.getStudentByPhone(
                        TenantContextHolder.getRequiredTenant(), phoneNumber)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND,
                        "입력한 전화번호와 일치하는 학생이 없습니다."));
        if (!student.isActive()) {
            submission.reject("INACTIVE_STUDENT_PHONE");
            throw new BusinessException(ExamErrorCode.INACTIVE_STUDENT_PHONE);
        }
        if (!student.id().equals(request.expectedStudentId())) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "학생 정보가 변경되었습니다. 전화번호를 다시 확인해 주세요.");
        }
        ExamResult existing = resultRepository.findByExamIdAndStudentId(examId, student.id()).orElse(null);
        Long currentResultId = existing == null ? null : existing.getId();
        if (!java.util.Objects.equals(currentResultId, request.expectedCurrentResultId())) {
            throw new BusinessException(ExamErrorCode.DUPLICATE_RESULT,
                    "기존 성적 정보가 변경되었습니다. 다시 확인해 주세요.");
        }
        if (existing != null && !request.replaceCurrentResult()) {
            throw new BusinessException(ExamErrorCode.DUPLICATE_RESULT,
                    "이미 해당 학생의 시험 성적이 있습니다. 교체 여부를 확인하세요.");
        }

        saveOfficialProjection(
                submission,
                student,
                existing,
                true,
                "이미 다른 요청이 이 학생 또는 OMR submission의 공식 성적을 확정했습니다. 새로고침 후 다시 확인하세요.");
        submission.correctPhone(phoneNumber);
        submission.markMatched(student.id());
        return responseFor(submission, student.studentName());
    }

    @Transactional
    public int linkSingleUnmatchedByPhone(String phoneNumber, Long studentId) {
        List<OmrSubmission> candidates = submissionRepository.findByEffectivePhoneNumberAndReconciliationStatus(
                phoneNumber, OmrReconciliationStatus.UNMATCHED);
        return candidates.stream()
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getExam().getId()))
                .values().stream()
                .mapToInt(sameExam -> linkCandidateGroup(sameExam, studentId))
                .sum();
    }

    private int linkCandidateGroup(List<OmrSubmission> candidates, Long studentId) {
        if (candidates.size() != 1) {
            candidates.forEach(OmrSubmission::markAmbiguous);
            return 0;
        }
        OmrSubmission submission = candidates.getFirst();
        ExamResult existing = resultRepository.findByExamIdAndStudentId(submission.getExam().getId(), studentId).orElse(null);
        if (existing != null) {
            submission.markAmbiguous();
            return 0;
        }
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        StudentService.StudentData student = studentService.getStudent(tenantSlug, studentId).orElse(null);
        if (student == null || !student.isActive()) {
            return 0;
        }
        submission.markMatched(studentId);
        saveOfficialProjection(
                submission,
                student,
                null,
                false,
                "이미 다른 요청이 이 학생 또는 OMR submission의 공식 성적을 확정했습니다.");
        return 1;
    }

    @Transactional(readOnly = true)
    public List<OmrSubmissionResponse> getJobSubmissions(Long examId, Long jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
        if (!job.getExam().getId().equals(examId)) {
            throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
        }
        return submissionRepository.findByJobIdOrderByImageIndexAsc(jobId).stream()
                .map(submission -> responseFor(submission, resolveStudentName(submission)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OmrSubmissionResponse> getReconciliationQueue(Long examId) {
        return submissionRepository.findByExamIdAndReconciliationStatusInOrderByCreatedAtDesc(
                        examId, List.of(OmrReconciliationStatus.UNMATCHED,
                                OmrReconciliationStatus.AMBIGUOUS, OmrReconciliationStatus.REJECTED))
                .stream()
                .map(submission -> responseFor(submission, resolveStudentName(submission)))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] getImage(Long examId, Long submissionId, com.lumie.exam.application.port.out.OmrStoragePort storagePort) {
        OmrSubmission submission = requireSubmission(examId, submissionId);
        if (submission.getOmrImageKey() == null) {
            throw new BusinessException(ExamErrorCode.OMR_IMAGE_NOT_FOUND);
        }
        return storagePort.getObject(submission.getOmrImageKey());
    }

    @Transactional(readOnly = true)
    public List<String> getSourceImageKeysWithoutPermanentCopy(Long jobId) {
        return submissionRepository.findSourceImageKeysWithoutPermanentCopyByJobId(jobId);
    }

    private OmrSubmission getSubmissionForUpdate(Long jobId, int imageIndex) {
        return submissionRepository.findByJobIdAndImageIndexForUpdate(jobId, imageIndex)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
    }

    private void applyAutomatic(OmrSubmission submission, StudentService.StudentData student) {
        ExamResult existing = resultRepository.findByExamIdAndStudentId(submission.getExam().getId(), student.id()).orElse(null);
        if (isProtectedByManualOverride(submission, existing)) {
            return;
        }
        if (existing != null && existing.getCurrentSubmissionId() != null) {
            OmrSubmission current = submissionRepository.findById(existing.getCurrentSubmissionId()).orElse(null);
            if (current != null && !submission.isNewerThan(current)) {
                return;
            }
        }
        try {
            resultRepository.saveAndFlush(applyToResult(submission, student, existing));
        } catch (DataIntegrityViolationException exception) {
            ExamResult raced = resultRepository.findByExamIdAndStudentId(submission.getExam().getId(), student.id())
                    .orElseThrow(() -> exception);
            OmrSubmission current = raced.getCurrentSubmissionId() == null ? null
                    : submissionRepository.findById(raced.getCurrentSubmissionId()).orElse(null);
            if (!isProtectedByManualOverride(submission, raced) && submission.isNewerThan(current)) {
                resultRepository.saveAndFlush(applyToResult(submission, student, raced));
            }
        }
    }

    private static boolean isProtectedByManualOverride(OmrSubmission submission, ExamResult result) {
        return result != null && result.getManualOverrideAt() != null
                && !submission.getCreatedAt().isAfter(result.getManualOverrideAt());
    }

    private ExamResult applyToResult(OmrSubmission submission, StudentService.StudentData student, ExamResult existing) {
        Exam exam = submission.getExam();
        OmrServicePort.OmrGradingResult gradingResult = new OmrServicePort.OmrGradingResult(
                submission.getTotalScore(), submission.getGrade(), submission.getOcrPhoneNumber(),
                submission.getQuestions().stream().map(question -> new OmrServicePort.OmrQuestionResult(
                        question.getQuestionNumber(), question.getStudentAnswer(), question.getCorrectAnswer(),
                        question.getMaxScore(), question.getEarnedScore(), question.getQuestionType())).toList());
        ExamResult result = existing == null
                ? resultCommandService.resolveResult(exam, student.id(), gradingResult)
                : resultCommandService.resolveResult(exam.getId(), exam, student.id(), gradingResult);
        result.assignPhoneNumber(student.studentPhone());
        result.applyOmrSubmission(submission.getId(), submission.getJobId(), submission.getOmrImageKey());
        return result;
    }

    private void saveOfficialProjection(
            OmrSubmission submission,
            StudentService.StudentData student,
            ExamResult existing,
            boolean manualOverride,
            String conflictMessage) {
        try {
            ExamResult result = applyToResult(submission, student, existing);
            if (manualOverride) {
                result.recordManualOverride(Instant.now());
            }
            resultRepository.saveAndFlush(result);
        } catch (DataIntegrityViolationException exception) {
            if (!isUniqueViolation(exception)) {
                throw exception;
            }
            throw new BusinessException(ExamErrorCode.DUPLICATE_RESULT, conflictMessage, exception);
        }
    }

    private OmrSubmission requireSubmission(Long examId, Long submissionId) {
        OmrSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
        if (!submission.getExam().getId().equals(examId)) {
            throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
        }
        return submission;
    }

    private OmrSubmission requireReconciliableSubmission(Long examId, Long submissionId) {
        OmrSubmission submission = requireSubmission(examId, submissionId);
        requireCompletedSubmission(submission);
        requireAvailableForOfficialProjection(submission, null);
        return submission;
    }

    private static String requireStudentPhone(String phoneNumber) {
        String normalized = PhoneNumberUtils.toDigits(phoneNumber);
        if (!PhoneNumberUtils.isValidStudentPhone(normalized)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "전화번호 형식이 올바르지 않습니다. (예: 01012345678)");
        }
        return normalized;
    }

    private void requireCompletedSubmission(OmrSubmission submission) {
        if (submission.getGradingStatus() != OmrGradingStatus.SUCCEEDED) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "완료된 OMR submission만 수동으로 처리할 수 있습니다.");
        }
    }

    private void requireAvailableForOfficialProjection(OmrSubmission submission, ExamResult targetResult) {
        ExamResult owner = resultRepository.findByCurrentSubmissionId(submission.getId()).orElse(null);
        if (owner != null && (targetResult == null || !owner.getId().equals(targetResult.getId()))) {
            throw new BusinessException(ExamErrorCode.DUPLICATE_RESULT,
                    "이 OMR submission은 이미 다른 학생의 공식 성적에 반영되었습니다.");
        }
    }

    private String resolveStudentName(OmrSubmission submission) {
        if (submission.getMatchedStudentId() == null) {
            return null;
        }
        return studentService.getStudent(TenantContextHolder.getRequiredTenant(), submission.getMatchedStudentId())
                .map(StudentService.StudentData::studentName)
                .orElse(null);
    }

    private OmrSubmissionResponse responseFor(OmrSubmission submission, String studentName) {
        boolean official = resultRepository.findByCurrentSubmissionId(submission.getId()).isPresent();
        return OmrSubmissionResponse.from(submission, studentName, official);
    }

    private static String normalizeCallbackPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        String normalized = PhoneNumberUtils.toDigits(phoneNumber);
        return normalized.startsWith("010") ? normalized : "010" + normalized;
    }

    private static boolean isUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        return cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState());
    }
}
