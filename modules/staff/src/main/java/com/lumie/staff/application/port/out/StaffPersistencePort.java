package com.lumie.staff.application.port.out;

import com.lumie.staff.domain.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StaffPersistencePort {
    Optional<Staff> findById(Long id);
    Optional<Staff> findByUserId(Long userId);
    Optional<Staff> findByUserLoginId(String userLoginId);
    boolean existsByUserLoginId(String userLoginId);
    boolean existsByCanonicalPhone(String phone);
    boolean existsByCanonicalPhoneAndIdNot(String phone, Long excludeId);
    long countByIsActive(Boolean isActive);
    List<Object[]> countGroupedByRoleAndIsActive(Boolean isActive);
    default List<Object[]> countActiveGroupedByRole() {
        return countGroupedByRoleAndIsActive(true);
    }
    Page<Staff> search(Boolean isActive, String search, Pageable pageable);
    Staff save(Staff staff);
    void delete(Staff staff);
}
