package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentJpaRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {
    List<Assignment> findByIdIn(Collection<Long> ids);
    @Query("""
            select distinct assignment from Assignment assignment
            where assignment.submissionMode = com.lumie.assignment.domain.vo.SubmissionMode.EXAM_MANUAL
              and exists (select target.id from AssignmentClassTarget target
                          where target.assignmentId = assignment.id and target.classId = :classId)
            """)
    Page<Assignment> findByTargetClassId(@Param("classId") Long classId, Pageable pageable);

    @Query("""
            select distinct assignment from Assignment assignment
            join AssignmentClassTarget target on target.assignmentId = assignment.id
            where assignment.status = com.lumie.assignment.domain.vo.AssignmentStatus.ACTIVE
              and assignment.dueDate > :now
              and assignment.submissionMode = com.lumie.assignment.domain.vo.SubmissionMode.EXAM_MANUAL
              and target.classId = :classId
            """)
    List<Assignment> findActiveByTargetClassId(@Param("classId") Long classId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Assignment assignment
            set assignment.status = :closedStatus, assignment.updatedAt = :now,
                assignment.version = assignment.version + 1
            where assignment.status = :activeStatus and assignment.dueDate <= :now
            """)
    int closeOverdueActiveAssignments(@Param("activeStatus") AssignmentStatus activeStatus,
            @Param("closedStatus") AssignmentStatus closedStatus, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Assignment assignment
            set assignment.status = :closedStatus, assignment.updatedAt = :now,
                assignment.version = assignment.version + 1
            where assignment.id = :assignmentId and assignment.status = :activeStatus
              and assignment.dueDate <= :now
            """)
    int closeOverdueActiveAssignment(@Param("assignmentId") Long assignmentId,
            @Param("activeStatus") AssignmentStatus activeStatus,
            @Param("closedStatus") AssignmentStatus closedStatus, @Param("now") Instant now);
}
