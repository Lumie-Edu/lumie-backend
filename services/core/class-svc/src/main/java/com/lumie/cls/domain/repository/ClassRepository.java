package com.lumie.cls.domain.repository;

import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.vo.ClassSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ClassRepository {

    ClassEntity save(ClassEntity classEntity);

    void delete(ClassEntity classEntity);

    Optional<ClassEntity> findById(Long id);

    Optional<ClassEntity> findByIdWithLock(Long id);

    Page<ClassEntity> findAll(ClassSearchCriteria criteria, Pageable pageable);

    Page<ClassEntity> findByAcademyId(Long academyId, Pageable pageable);

    Page<ClassEntity> findByTeacherId(Long teacherId, Pageable pageable);

    List<ClassEntity> findAllByTeacherId(Long teacherId);

    long countByAcademyId(Long academyId);

    long count();
}
