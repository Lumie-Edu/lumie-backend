package com.lumie.assignment.domain.repository;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    Page<AssignmentSubmission> findByAssignmentId(Long assignmentId, Pageable pageable);

    List<AssignmentSubmission> findAllByAssignmentId(Long assignmentId);

    Page<AssignmentSubmission> findByStudentId(Long studentId, Pageable pageable);

    Page<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId, Pageable pageable);

    Optional<AssignmentSubmission> findFirstByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @Query("SELECT s.status, COUNT(s) FROM AssignmentSubmission s GROUP BY s.status")
    List<Object[]> countGroupedByStatus();
}
