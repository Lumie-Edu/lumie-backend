package com.lumie.assignment.domain.repository;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentTarget;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentTargetType;
import com.lumie.assignment.domain.vo.SubmissionMode;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long>,
        JpaSpecificationExecutor<Assignment> {

    @Query("""
            select distinct assignment
            from Assignment assignment
            where assignment.submissionMode = com.lumie.assignment.domain.vo.SubmissionMode.EXAM_MANUAL
              and (
                assignment.classId = :classId
                or exists (
                select target.id
                from AssignmentTarget target
                where target.assignmentId = assignment.id
                  and target.targetType = com.lumie.assignment.domain.vo.AssignmentTargetType.CLASS
                  and target.targetId = :classId
                )
              )
            """)
    Page<Assignment> findByTargetClassId(@Param("classId") Long classId, Pageable pageable);

    List<Assignment> findByIdIn(Collection<Long> ids);

    default Page<Assignment> findAll(AssignmentSearchCriteria criteria, Pageable pageable) {
        Specification<Assignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("submissionMode"), SubmissionMode.EXAM_MANUAL));
            if (criteria.classId() != null) {
                Subquery<Long> targetExists = query.subquery(Long.class);
                Root<AssignmentTarget> target = targetExists.from(AssignmentTarget.class);
                targetExists.select(target.get("id"));
                targetExists.where(
                        cb.equal(target.get("assignmentId"), root.get("id")),
                        cb.equal(target.get("targetType"), AssignmentTargetType.CLASS),
                        cb.equal(target.get("targetId"), criteria.classId())
                );
                predicates.add(cb.or(
                        cb.equal(root.get("classId"), criteria.classId()),
                        cb.exists(targetExists)
                ));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
