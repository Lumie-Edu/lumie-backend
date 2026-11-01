package com.lumie.staff.application.port.out;

import com.lumie.staff.domain.entity.StaffPermission;

import java.util.List;

public interface StaffPermissionPersistencePort {
    List<StaffPermission> findByStaffId(Long staffId);
    void deleteByStaffId(Long staffId);
    List<StaffPermission> saveAll(Iterable<StaffPermission> entries);
}
