package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.domain.entity.StaffPermission;
import com.lumie.staff.domain.entity.StaffPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffPermissionJpaRepository extends JpaRepository<StaffPermission, StaffPermissionId> {
    List<StaffPermission> findByStaffId(Long staffId);
    void deleteByStaffId(Long staffId);
}
