package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaAdminRepository extends JpaRepository<Admin, Long>, JpaSpecificationExecutor<Admin> {
    Optional<Admin> findByUserId(Long userId);
    Optional<Admin> findByUserLoginId(String userLoginId);
    boolean existsByUserLoginId(String userLoginId);
    boolean existsByPhone(String phone);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Admin a WHERE a.phone = :phone AND a.id != :excludeId")
    boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    long countByIsActive(Boolean isActive);

    @Query("SELECT a.role, COUNT(a) FROM Admin a WHERE a.isActive = :isActive GROUP BY a.role")
    List<Object[]> countGroupedByRoleAndIsActive(@Param("isActive") Boolean isActive);
}
