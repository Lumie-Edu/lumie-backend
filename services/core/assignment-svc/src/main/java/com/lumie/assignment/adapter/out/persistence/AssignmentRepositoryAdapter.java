package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AssignmentRepositoryAdapter implements AssignmentRepository {

    private final JpaAssignmentRepository jpaRepository;

    @Override
    public Assignment save(Assignment assignment) {
        return jpaRepository.save(assignment);
    }

    @Override
    public Optional<Assignment> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Assignment> findAll(AssignmentSearchCriteria criteria, Pageable pageable) {
        Specification<Assignment> spec = toSpecification(criteria);
        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Assignment> findByClassId(Long classId, Pageable pageable) {
        return jpaRepository.findByClassId(classId, pageable);
    }

    private Specification<Assignment> toSpecification(AssignmentSearchCriteria criteria) {
        Specification<Assignment> spec = Specification.where(null);

        if (criteria.classId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("classId"), criteria.classId()));
        }
        if (criteria.type() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("type"), criteria.type()));
        }
        if (criteria.status() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + criteria.search().toLowerCase() + "%"));
        }

        return spec;
    }
}
