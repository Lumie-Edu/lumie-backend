package com.lumie.staff.domain.repository;

import com.lumie.staff.domain.entity.StaffPermission;
import com.lumie.staff.domain.entity.StaffPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffPermissionRepository extends JpaRepository<StaffPermission, StaffPermissionId> {

    List<StaffPermission> findByStaffId(Long staffId);

    void deleteByStaffId(Long staffId);
}
