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
import java.util.Locale;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>,
        JpaSpecificationExecutor<Student> {

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

    /**
     * Counts deactivated students whose {@code updated_at} falls within [from, to).
     *
     * <p><b>Fallback note:</b> the {@code students} table has no {@code deactivated_at}
     * column; {@code updated_at} is used as a proxy for the deactivation timestamp.
     * This is accurate only when the last mutation on the row was the deactivation.
     * A dedicated {@code deactivated_at} column (Flyway migration) would make this
     * precise. Track in: TODO add deactivated_at to students table.
     */
    long countByIsActiveAndUpdatedAtBetween(Boolean isActive, Instant from, Instant to);

    long countByIsActiveAndUpdatedAtBefore(Boolean isActive, Instant cutoff);

    /**
     * Returns monthly dropout counts (rows where {@code is_active = false}) grouped by
     * the month of {@code updated_at} for all rows updated on or after {@code from}.
     *
     * <p>Same {@code updated_at} proxy caveat as {@link #countByIsActiveAndUpdatedAtBetween}.
     */
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

    /**
     * Searches active/inactive students by free-text keyword across name, school,
     * phones, and birth year. Composes a Specification inline.
     */
    default Page<Student> search(
            Boolean isActive,
            String search,
            Collection<Long> includedStudentIds,
            Collection<Long> excludedStudentIds,
            Pageable pageable
    ) {
        Specification<Student> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("canonicalName")), pattern),
                        cb.like(cb.lower(root.get("studentHighschool")), pattern),
                        cb.like(root.get("canonicalPhone"), "%" + search + "%"),
                        cb.like(root.get("parentPhone"), "%" + search + "%"),
                        cb.like(cb.toString(root.get("studentBirthYear")), "%" + search + "%")
                ));
            }
            if (includedStudentIds != null && !includedStudentIds.isEmpty()) {
                predicates.add(root.get("id").in(includedStudentIds));
            }
            if (excludedStudentIds != null && !excludedStudentIds.isEmpty()) {
                predicates.add(cb.not(root.get("id").in(excludedStudentIds)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
