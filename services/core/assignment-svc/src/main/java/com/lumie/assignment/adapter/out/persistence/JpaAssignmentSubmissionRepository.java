package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaAssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    Page<AssignmentSubmission> findByAssignmentId(Long assignmentId, Pageable pageable);

    Page<AssignmentSubmission> findByStudentId(Long studentId, Pageable pageable);

    Page<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId, Pageable pageable);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @Query("SELECT s.status, COUNT(s) FROM AssignmentSubmission s GROUP BY s.status")
    List<Object[]> countGroupedByStatus();
}
