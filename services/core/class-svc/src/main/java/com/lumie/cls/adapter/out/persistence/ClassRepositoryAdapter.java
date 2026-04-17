package com.lumie.cls.adapter.out.persistence;

import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.repository.ClassRepository;
import com.lumie.cls.domain.vo.ClassSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClassRepositoryAdapter implements ClassRepository {

    private final JpaClassRepository jpaRepository;

    @Override
    public ClassEntity save(ClassEntity classEntity) {
        return jpaRepository.save(classEntity);
    }

    @Override
    public void delete(ClassEntity classEntity) {
        jpaRepository.delete(classEntity);
    }

    @Override
    public Optional<ClassEntity> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<ClassEntity> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id);
    }

    @Override
    public Page<ClassEntity> findAll(ClassSearchCriteria criteria, Pageable pageable) {
        Specification<ClassEntity> spec = toSpecification(criteria);
        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ClassEntity> findByAcademyId(Long academyId, Pageable pageable) {
        return jpaRepository.findByAcademyId(academyId, pageable);
    }

    @Override
    public Page<ClassEntity> findByTeacherId(Long teacherId, Pageable pageable) {
        return jpaRepository.findByTeacherId(teacherId, pageable);
    }

    @Override
    public List<ClassEntity> findAllByTeacherId(Long teacherId) {
        return jpaRepository.findAllByTeacherId(teacherId);
    }

    @Override
    public long countByAcademyId(Long academyId) {
        return jpaRepository.countByAcademyId(academyId);
    }

    @Override
    public boolean existsByNameAndAcademyId(String name, Long academyId) {
        return jpaRepository.existsByNameAndAcademyId(name, academyId);
    }

    private Specification<ClassEntity> toSpecification(ClassSearchCriteria criteria) {
        Specification<ClassEntity> spec = Specification.where(null);

        if (criteria.academyId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("academyId"), criteria.academyId()));
        }
        if (criteria.teacherId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("teacherId"), criteria.teacherId()));
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + criteria.search().toLowerCase() + "%"));
        }

        return spec;
    }
}
