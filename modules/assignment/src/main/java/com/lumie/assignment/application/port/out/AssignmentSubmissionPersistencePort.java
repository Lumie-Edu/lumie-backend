package com.lumie.assignment.application.port.out;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssignmentSubmissionPersistencePort {
    Page<AssignmentSubmission> findByAssignmentId(Long assignmentId, Pageable pageable);
    List<AssignmentSubmission> findAllByAssignmentId(Long assignmentId);
    List<AssignmentSubmission> findAllByAssignmentIdIn(Collection<Long> assignmentIds);
    Page<AssignmentSubmission> findByStudentId(Long studentId, Pageable pageable);
    Page<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId, Pageable pageable);
    Optional<AssignmentSubmission> findFirstByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<Long> findAssignmentIdsByStudentId(Long studentId);
    List<Object[]> countGroupedByStatus();
    List<Object[]> countGroupedByStatusSince(Instant from);
    Optional<AssignmentSubmission> findById(Long id);
    AssignmentSubmission save(AssignmentSubmission submission);
}
