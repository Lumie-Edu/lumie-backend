package com.lumie.assignment.domain.repository;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long>,
        JpaSpecificationExecutor<Assignment> {

    Page<Assignment> findByClassId(Long classId, Pageable pageable);

    default Page<Assignment> findAll(AssignmentSearchCriteria criteria, Pageable pageable) {
        Specification<Assignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.classId() != null) {
                predicates.add(cb.equal(root.get("classId"), criteria.classId()));
            }
            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.type()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.search() != null && !criteria.search().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + criteria.search().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
