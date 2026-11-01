package com.lumie.classroom.adapter.out.persistence;

import com.lumie.classroom.application.port.out.ClassPersistencePort;
import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.vo.ClassSearchCriteria;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import jakarta.persistence.criteria.Predicate;

@Component
@RequiredArgsConstructor
public class ClassPersistenceAdapter implements ClassPersistencePort {
    private final ClassJpaRepository repository;

    @Override public Optional<ClassEntity> findByIdWithLock(Long id) { return repository.findByIdWithLock(id); }
    @Override public Page<ClassEntity> findByTeacherId(Long teacherId, Pageable pageable) {
        return repository.findByTeacherId(teacherId, pageable);
    }
    @Override public Page<ClassEntity> findEnrolledClassesByStudentIdAndStatus(
            Long studentId, EnrollmentStatus status, Pageable pageable) {
        return repository.findEnrolledClassesByStudentIdAndStatus(studentId, status, pageable);
    }
    @Override public List<ClassEntity> findAllByTeacherId(Long teacherId) {
        return repository.findAllByTeacherId(teacherId);
    }
    @Override public boolean existsByTeacherId(Long teacherId) { return repository.existsByTeacherId(teacherId); }
    @Override public Page<ClassEntity> findAll(ClassSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(specification(criteria), pageable);
    }
    @Override public Optional<ClassEntity> findById(Long id) { return repository.findById(id); }
    @Override public ClassEntity save(ClassEntity entity) { return repository.save(entity); }
    @Override public void delete(ClassEntity entity) { repository.delete(entity); }
    @Override public List<ClassEntity> findAllById(Iterable<Long> ids) { return repository.findAllById(ids); }
    @Override public long count() { return repository.count(); }

    private Specification<ClassEntity> specification(ClassSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.teacherId() != null) {
                predicates.add(builder.equal(root.get("teacherId"), criteria.teacherId()));
            }
            if (criteria.search() != null && !criteria.search().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("name")),
                        "%" + criteria.search().toLowerCase(Locale.ROOT) + "%"));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
