package com.lumie.staff.domain.repository;

import com.lumie.staff.domain.entity.Staff;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long>, JpaSpecificationExecutor<Staff> {

    Optional<Staff> findByUserId(Long userId);

    Optional<Staff> findByUserLoginId(String userLoginId);

    boolean existsByUserLoginId(String userLoginId);

    boolean existsByCanonicalPhone(String phone);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Staff s WHERE s.canonicalPhone = :phone AND s.id != :excludeId")
    boolean existsByCanonicalPhoneAndIdNot(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    long countByIsActive(Boolean isActive);

    @Query("SELECT s.role, COUNT(s) FROM Staff s WHERE s.isActive = :isActive GROUP BY s.role")
    List<Object[]> countGroupedByRoleAndIsActive(@Param("isActive") Boolean isActive);

    /** Convenience wrapper: counts active staff grouped by role. */
    default List<Object[]> countActiveGroupedByRole() {
        return countGroupedByRoleAndIsActive(true);
    }

    default Page<Staff> search(Boolean isActive, String search, Pageable pageable) {
        Specification<Staff> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("canonicalName")), pattern),
                        cb.like(cb.lower(root.get("userLoginId")), pattern),
                        cb.like(cb.lower(root.get("canonicalEmail")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
