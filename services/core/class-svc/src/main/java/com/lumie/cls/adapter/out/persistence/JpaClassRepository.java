package com.lumie.cls.adapter.out.persistence;

import com.lumie.cls.domain.entity.ClassEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaClassRepository extends JpaRepository<ClassEntity, Long>, JpaSpecificationExecutor<ClassEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassEntity c WHERE c.id = :id")
    Optional<ClassEntity> findByIdWithLock(@Param("id") Long id);

    Page<ClassEntity> findByAcademyId(Long academyId, Pageable pageable);

    Page<ClassEntity> findByTeacherId(Long teacherId, Pageable pageable);

    List<ClassEntity> findAllByTeacherId(Long teacherId);

    long countByAcademyId(Long academyId);

    boolean existsByNameAndAcademyId(String name, Long academyId);
}
