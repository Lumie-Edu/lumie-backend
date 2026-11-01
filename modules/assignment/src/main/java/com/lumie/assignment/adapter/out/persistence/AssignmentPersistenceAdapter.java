package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.application.port.out.AssignmentPersistencePort;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentClassTarget;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.SubmissionMode;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AssignmentPersistenceAdapter implements AssignmentPersistencePort {
    private final AssignmentJpaRepository repository;

    @Override public Page<Assignment> findByTargetClassId(Long classId, Pageable pageable) { return repository.findByTargetClassId(classId, pageable); }
    @Override public List<Assignment> findByIdIn(Collection<Long> ids) { return repository.findByIdIn(ids); }
    @Override public List<Assignment> findActiveByTargetClassId(Long classId, Instant now) { return repository.findActiveByTargetClassId(classId, now); }

    @Override
    @Transactional
    public int closeOverdueActiveAssignments(Instant now) {
        return repository.closeOverdueActiveAssignments(AssignmentStatus.ACTIVE, AssignmentStatus.CLOSED, now);
    }

    @Override
    @Transactional
    public int closeOverdueActiveAssignment(Long assignmentId, Instant now) {
        return repository.closeOverdueActiveAssignment(assignmentId, AssignmentStatus.ACTIVE, AssignmentStatus.CLOSED, now);
    }

    @Override
    public Page<Assignment> findAll(AssignmentSearchCriteria criteria, Pageable pageable) {
        Specification<Assignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("submissionMode"), SubmissionMode.EXAM_MANUAL));
            if (criteria.classId() != null) {
                Subquery<Long> targetExists = query.subquery(Long.class);
                Root<AssignmentClassTarget> target = targetExists.from(AssignmentClassTarget.class);
                targetExists.select(target.get("id"));
                targetExists.where(cb.equal(target.get("assignmentId"), root.get("id")),
                        cb.equal(target.get("classId"), criteria.classId()));
                predicates.add(cb.exists(targetExists));
            }
            if (criteria.status() == AssignmentStatus.ACTIVE) {
                predicates.add(cb.and(cb.equal(root.get("status"), AssignmentStatus.ACTIVE),
                        cb.greaterThan(root.get("dueDate"), criteria.now())));
            } else if (criteria.status() == AssignmentStatus.CLOSED) {
                predicates.add(cb.or(cb.equal(root.get("status"), AssignmentStatus.CLOSED),
                        cb.and(cb.equal(root.get("status"), AssignmentStatus.ACTIVE),
                                cb.lessThanOrEqualTo(root.get("dueDate"), criteria.now()))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    @Override public Optional<Assignment> findById(Long id) { return repository.findById(id); }
    @Override public Assignment save(Assignment assignment) { return repository.save(assignment); }
    @Override public void delete(Assignment assignment) { repository.delete(assignment); }
    @Override public void flush() { repository.flush(); }
}
