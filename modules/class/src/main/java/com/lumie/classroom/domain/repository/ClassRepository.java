package com.lumie.classroom.domain.repository;

import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.vo.ClassSearchCriteria;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Long>,
        JpaSpecificationExecutor<ClassEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassEntity c WHERE c.id = :id")
    Optional<ClassEntity> findByIdWithLock(@Param("id") Long id);

    Page<ClassEntity> findByTeacherId(Long teacherId, Pageable pageable);

    List<ClassEntity> findAllByTeacherId(Long teacherId);

    default Page<ClassEntity> findAll(ClassSearchCriteria criteria, Pageable pageable) {
        Specification<ClassEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.teacherId() != null) {
                predicates.add(cb.equal(root.get("teacherId"), criteria.teacherId()));
            }
            if (criteria.search() != null && !criteria.search().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + criteria.search().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
