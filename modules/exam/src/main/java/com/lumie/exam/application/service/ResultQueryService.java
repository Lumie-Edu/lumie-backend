package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.OmrJobStatusResponse;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.dto.response.statistics.TopStudentMonthlyResponse;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResultQueryService {

    private static final String DELETED_STUDENT_NAME = "삭제된 학생";

    private final ExamResultRepository resultRepository;
    private final OmrGradingJobRepository jobRepository;
    private final OmrStoragePort storagePort;
    private final StudentService studentService;

    public List<ExamResultResponse> getExamResults(Long examId) {
        log.debug("Getting results for exam: {}", examId);

        List<ExamResult> results = resultRepository.findByExamId(examId);
        Map<Long, String> nameByStudentId = resolveStudentNames(results);

        return results.stream()
                .map(r -> ExamResultResponse.from(r, resolveStudentName(r, nameByStudentId)))
                .toList();
    }

    public List<ExamResultResponse> getOmrJobResults(Long examId, Long jobId) {
        log.debug("Getting OMR job results: examId={}, jobId={}", examId, jobId);

        // Defense in depth: ensure the job actually belongs to this exam before
        // surfacing rows. Prevents URL-tampering across tenants/exams (RLS catches
        // cross-tenant, but cross-exam-within-tenant deserves a 404).
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
        if (!job.getExam().getId().equals(examId)) {
            throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
        }

        List<ExamResult> results = resultRepository.findByOmrGradingJobId(jobId);
        Map<Long, String> nameByStudentId = resolveStudentNames(results);

        return results.stream()
                .map(r -> ExamResultResponse.from(r, resolveStudentName(r, nameByStudentId)))
                .toList();
    }

    public List<ExamResultResponse> getStudentResults(Long studentId) {
        log.debug("Getting results for student: {}", studentId);

        List<ExamResult> results = resultRepository.findByStudentId(studentId);
        Map<Long, String> nameByStudentId = resolveStudentNames(results);

        return results.stream()
                .map(r -> ExamResultResponse.from(r, resolveStudentName(r, nameByStudentId)))
                .toList();
    }

    /**
     * Batched student-name lookup for a result list — single internal-api call
     * even when the list contains hundreds of rows. Null/unregistered student_id
     * entries are skipped so the response keeps unregistered/deleted fallback semantics.
     */
    private Map<Long, String> resolveStudentNames(List<ExamResult> results) {
        List<Long> studentIds = results.stream()
                .map(ExamResult::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return studentService.getStudentsByIds(tenantSlug, studentIds).students().stream()
                .collect(Collectors.toMap(
                        StudentService.StudentData::id,
                        StudentService.StudentData::studentName
                ));
    }

    private static String resolveStudentName(ExamResult result, Map<Long, String> nameByStudentId) {
        if (result.isStudentDeleted()) {
            return DELETED_STUDENT_NAME;
        }
        if (result.getStudentId() == null) {
            return result.getPhoneNumber();
        }
        return nameByStudentId.getOrDefault(result.getStudentId(), DELETED_STUDENT_NAME);
    }

    public List<TopStudentMonthlyResponse> getTopStudentsMonthly(int limit) {
        log.debug("Getting top {} students for current month", limit);

        var rows = resultRepository.findTopStudentsForCurrentMonth(limit);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = rows.stream()
                .map(r -> r.getStudentId())
                .filter(Objects::nonNull)
                .toList();
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Map<Long, String> nameById = studentService.getStudentsByIds(tenantSlug, studentIds).students().stream()
                .collect(Collectors.toMap(
                        StudentService.StudentData::id,
                        StudentService.StudentData::studentName
                ));
        return rows.stream()
                .map(r -> new TopStudentMonthlyResponse(
                        r.getStudentId(),
                        nameById.getOrDefault(r.getStudentId(), DELETED_STUDENT_NAME),
                        r.getAvgScore(),
                        r.getAttemptCount()
                ))
                .toList();
    }

    public List<QuestionResultResponse> getQuestionResults(Long resultId) {
        log.debug("Getting question results for result: {}", resultId);

        ExamResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        var exam = result.getExam();
        return result.getQuestionResults().stream()
                .map(qr -> QuestionResultResponse.from(qr, exam))
                .toList();
    }

    public OmrJobStatusResponse getOmrJobStatus(Long examId, Long jobId) {
        log.debug("Getting OMR job status: examId={}, jobId={}", examId, jobId);

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
        if (!job.getExam().getId().equals(examId)) {
            throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
        }
        return OmrJobStatusResponse.from(job);
    }

    public List<OmrJobStatusResponse> getOmrJobs(Long examId) {
        log.debug("Getting OMR jobs for exam: {}", examId);

        return jobRepository.findByExamIdOrderByCreatedAtDesc(examId).stream()
                .map(OmrJobStatusResponse::from)
                .toList();
    }

    public byte[] getOmrImageData(Long resultId) {
        ExamResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        if (result.getOmrImageKey() == null) {
            throw new BusinessException(ExamErrorCode.OMR_IMAGE_NOT_FOUND);
        }

        return storagePort.getObject(result.getOmrImageKey());
    }
}
