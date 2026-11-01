package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentSubmissionJpaRepository extends JpaRepository<AssignmentSubmission, Long> {
    Page<AssignmentSubmission> findByAssignmentId(Long assignmentId, Pageable pageable);
    List<AssignmentSubmission> findAllByAssignmentId(Long assignmentId);
    List<AssignmentSubmission> findAllByAssignmentIdIn(Collection<Long> assignmentIds);
    Page<AssignmentSubmission> findByStudentId(Long studentId, Pageable pageable);
    Page<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId, Pageable pageable);
    Optional<AssignmentSubmission> findFirstByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @Query("select distinct submission.assignmentId from AssignmentSubmission submission where submission.studentId = :studentId")
    List<Long> findAssignmentIdsByStudentId(@Param("studentId") Long studentId);

    @Query("select s.status, count(s) from AssignmentSubmission s group by s.status")
    List<Object[]> countGroupedByStatus();

    @Query("select s.status, count(s) from AssignmentSubmission s where s.createdAt >= :from group by s.status")
    List<Object[]> countGroupedByStatusSince(@Param("from") Instant from);
}
