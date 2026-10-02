package com.lumie.assignment.domain.repository;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentClassTarget;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.SubmissionMode;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long>,
        JpaSpecificationExecutor<Assignment> {

    @Query("""
            select distinct assignment
            from Assignment assignment
            where assignment.submissionMode = com.lumie.assignment.domain.vo.SubmissionMode.EXAM_MANUAL
              and exists (
                select target.id
                from AssignmentClassTarget target
                where target.assignmentId = assignment.id
                  and target.classId = :classId
              )
            """)
    Page<Assignment> findByTargetClassId(@Param("classId") Long classId, Pageable pageable);

    List<Assignment> findByIdIn(Collection<Long> ids);

    @Query("""
            select distinct assignment
            from Assignment assignment
            join AssignmentClassTarget target on target.assignmentId = assignment.id
            where assignment.status = com.lumie.assignment.domain.vo.AssignmentStatus.ACTIVE
              and assignment.dueDate > :now
              and assignment.submissionMode = com.lumie.assignment.domain.vo.SubmissionMode.EXAM_MANUAL
              and target.classId = :classId
            """)
    List<Assignment> findActiveByTargetClassId(@Param("classId") Long classId, @Param("now") Instant now);

    default int closeOverdueActiveAssignments(Instant now) {
        return closeOverdueActiveAssignments(AssignmentStatus.ACTIVE, AssignmentStatus.CLOSED, now);
    }

    default int closeOverdueActiveAssignment(Long assignmentId, Instant now) {
        return closeOverdueActiveAssignment(assignmentId, AssignmentStatus.ACTIVE, AssignmentStatus.CLOSED, now);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Assignment assignment
            set assignment.status = :closedStatus
              , assignment.updatedAt = :now
              , assignment.version = assignment.version + 1
            where assignment.status = :activeStatus
              and assignment.dueDate <= :now
            """)
    int closeOverdueActiveAssignments(
            @Param("activeStatus") AssignmentStatus activeStatus,
            @Param("closedStatus") AssignmentStatus closedStatus,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Assignment assignment
            set assignment.status = :closedStatus
              , assignment.updatedAt = :now
              , assignment.version = assignment.version + 1
            where assignment.id = :assignmentId
              and assignment.status = :activeStatus
              and assignment.dueDate <= :now
            """)
    int closeOverdueActiveAssignment(
            @Param("assignmentId") Long assignmentId,
            @Param("activeStatus") AssignmentStatus activeStatus,
            @Param("closedStatus") AssignmentStatus closedStatus,
            @Param("now") Instant now);

    default Page<Assignment> findAll(AssignmentSearchCriteria criteria, Pageable pageable) {
        Specification<Assignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("submissionMode"), SubmissionMode.EXAM_MANUAL));
            if (criteria.classId() != null) {
                Subquery<Long> targetExists = query.subquery(Long.class);
                Root<AssignmentClassTarget> target = targetExists.from(AssignmentClassTarget.class);
                targetExists.select(target.get("id"));
                targetExists.where(
                        cb.equal(target.get("assignmentId"), root.get("id")),
                        cb.equal(target.get("classId"), criteria.classId())
                );
                predicates.add(cb.exists(targetExists));
            }
            if (criteria.status() != null) {
                if (criteria.status() == AssignmentStatus.ACTIVE) {
                    predicates.add(cb.and(
                            cb.equal(root.get("status"), AssignmentStatus.ACTIVE),
                            cb.greaterThan(root.get("dueDate"), criteria.now())
                    ));
                } else if (criteria.status() == AssignmentStatus.CLOSED) {
                    predicates.add(cb.or(
                            cb.equal(root.get("status"), AssignmentStatus.CLOSED),
                            cb.and(
                                    cb.equal(root.get("status"), AssignmentStatus.ACTIVE),
                                    cb.lessThanOrEqualTo(root.get("dueDate"), criteria.now())
                            )
                    ));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
