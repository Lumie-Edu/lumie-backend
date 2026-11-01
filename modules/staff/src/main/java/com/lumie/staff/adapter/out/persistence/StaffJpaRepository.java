package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.domain.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffJpaRepository extends JpaRepository<Staff, Long>, JpaSpecificationExecutor<Staff> {
    Optional<Staff> findByUserId(Long userId);
    Optional<Staff> findByUserLoginId(String userLoginId);
    boolean existsByUserLoginId(String userLoginId);
    boolean existsByCanonicalPhone(String phone);
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Staff s WHERE s.canonicalPhone = :phone AND s.id != :excludeId")
    boolean existsByCanonicalPhoneAndIdNot(@Param("phone") String phone, @Param("excludeId") Long excludeId);
    long countByIsActive(Boolean isActive);
    @Query("SELECT s.role, COUNT(s) FROM Staff s WHERE s.isActive = :isActive GROUP BY s.role")
    List<Object[]> countGroupedByRoleAndIsActive(@Param("isActive") Boolean isActive);
}
