package com.lumie.assignment.domain.repository;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AssignmentSubmissionRepository {

    AssignmentSubmission save(AssignmentSubmission submission);

    Optional<AssignmentSubmission> findById(Long id);

    Page<AssignmentSubmission> findByAssignmentId(Long assignmentId, Pageable pageable);

    Page<AssignmentSubmission> findByStudentId(Long studentId, Pageable pageable);

    Page<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId, Pageable pageable);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
