package com.lumie.assignment.application.service;

import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.dto.response.SubmissionResponse;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.common.exception.BusinessException;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentQueryService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public AssignmentResponse getAssignment(Long id) {
        return assignmentRepository.findById(id)
            .map(AssignmentResponse::from)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND,
                "Assignment not found with id: %d".formatted(id)));
    }

    public Page<AssignmentResponse> listAssignments(Long classId,
                                                     AssignmentType type, AssignmentStatus status,
                                                     String search, Pageable pageable) {
        AssignmentSearchCriteria criteria = new AssignmentSearchCriteria(classId, type, status, search);
        return assignmentRepository.findAll(criteria, pageable)
            .map(AssignmentResponse::from);
    }

    public Page<AssignmentResponse> getAssignmentsByClass(Long classId, Pageable pageable) {
        return assignmentRepository.findByClassId(classId, pageable)
            .map(AssignmentResponse::from);
    }

    public Page<SubmissionResponse> getSubmissions(Long assignmentId, Pageable pageable) {
        return submissionRepository.findByAssignmentId(assignmentId, pageable)
            .map(SubmissionResponse::from);
    }

    public Page<SubmissionResponse> getStudentSubmissions(Long assignmentId, Long studentId, Pageable pageable) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId, pageable)
            .map(SubmissionResponse::from);
    }
}
