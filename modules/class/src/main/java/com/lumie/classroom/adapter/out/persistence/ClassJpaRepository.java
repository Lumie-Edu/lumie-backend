package com.lumie.classroom.adapter.out.persistence;

import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ClassJpaRepository extends JpaRepository<ClassEntity, Long>, JpaSpecificationExecutor<ClassEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassEntity c WHERE c.id = :id")
    Optional<ClassEntity> findByIdWithLock(@Param("id") Long id);

    Page<ClassEntity> findByTeacherId(Long teacherId, Pageable pageable);

    @Query(value = """
        SELECT c
          FROM ClassEntity c
         WHERE c.id IN (
               SELECT e.classEntity.id
                 FROM ClassEnrollment e
                WHERE e.studentId = :studentId
                  AND e.status = :status
         )
        """, countQuery = """
        SELECT COUNT(e)
          FROM ClassEnrollment e
         WHERE e.studentId = :studentId
           AND e.status = :status
        """)
    Page<ClassEntity> findEnrolledClassesByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") EnrollmentStatus status,
            Pageable pageable);

    List<ClassEntity> findAllByTeacherId(Long teacherId);
    boolean existsByTeacherId(Long teacherId);
}
