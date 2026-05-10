package com.lumie.student.domain.repository;

import com.lumie.student.domain.entity.Student;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>,
        JpaSpecificationExecutor<Student> {

    Optional<Student> findByUserId(Long userId);

    List<Student> findByUserIdIn(Collection<Long> userIds);

    Optional<Student> findByUserLoginId(String userLoginId);

    Optional<Student> findByPhone(String phone);

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

    boolean existsByUserLoginId(String userLoginId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Student s WHERE s.phone = :phone OR s.parentPhone = :phone")
    boolean existsByPhoneInAnyField(@Param("phone") String phone);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Student s WHERE (s.phone = :phone OR s.parentPhone = :phone) AND s.id != :excludeId")
    boolean existsByPhoneInAnyFieldExcluding(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    /**
     * Searches active/inactive students by free-text keyword across name, school,
     * phones, and birth year. Composes a Specification inline.
     */
    default Page<Student> search(Boolean isActive, String search, Pageable pageable) {
        Specification<Student> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("studentHighschool")), pattern),
                        cb.like(root.get("phone"), "%" + search + "%"),
                        cb.like(root.get("parentPhone"), "%" + search + "%"),
                        cb.like(cb.toString(root.get("studentBirthYear")), "%" + search + "%")
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
