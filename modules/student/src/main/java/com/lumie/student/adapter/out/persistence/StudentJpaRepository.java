package com.lumie.student.adapter.out.persistence;

import com.lumie.student.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface StudentJpaRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByUserId(Long userId);

    List<Student> findByUserIdIn(Collection<Long> userIds);

    Optional<Student> findByUserLoginId(String userLoginId);

    Optional<Student> findByCanonicalPhone(String phone);

    Page<Student> findByIsActive(Boolean isActive, Pageable pageable);

    long countByIsActive(Boolean isActive);

    long countByCreatedAtBetween(Instant from, Instant to);

    long countByCreatedAtBefore(Instant cutoff);

    @Query(value = """
            SELECT to_char(date_trunc('month', s.created_at), 'YYYY-MM') AS month,
                   COUNT(*)                                               AS new_count
            FROM students s
            WHERE s.created_at >= :from
            GROUP BY date_trunc('month', s.created_at)
            ORDER BY date_trunc('month', s.created_at)
            """, nativeQuery = true)
    List<Object[]> countMonthlyEnrollmentsSince(@Param("from") Instant from);

    long countByIsActiveAndUpdatedAtBetween(Boolean isActive, Instant from, Instant to);

    long countByIsActiveAndUpdatedAtBefore(Boolean isActive, Instant cutoff);

    @Query(value = """
            SELECT to_char(date_trunc('month', s.updated_at), 'YYYY-MM') AS month,
                   COUNT(*)                                               AS dropout_count
            FROM students s
            WHERE s.is_active = false
              AND s.updated_at >= :from
            GROUP BY date_trunc('month', s.updated_at)
            ORDER BY date_trunc('month', s.updated_at)
            """, nativeQuery = true)
    List<Object[]> countMonthlyDropoutsSince(@Param("from") Instant from);

    boolean existsByUserLoginId(String userLoginId);

    boolean existsByCanonicalPhone(String phone);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Student s WHERE s.canonicalPhone = :phone AND s.id != :excludeId")
    boolean existsByCanonicalPhoneExcluding(@Param("phone") String phone, @Param("excludeId") Long excludeId);
}
