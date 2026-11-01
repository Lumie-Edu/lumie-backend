package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.application.port.out.StaffPermissionPersistencePort;
import com.lumie.staff.domain.entity.StaffPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StaffPermissionPersistenceAdapter implements StaffPermissionPersistencePort {
    private final StaffPermissionJpaRepository repository;
    @Override public List<StaffPermission> findByStaffId(Long staffId) { return repository.findByStaffId(staffId); }
    @Override public void deleteByStaffId(Long staffId) { repository.deleteByStaffId(staffId); }
    @Override public List<StaffPermission> saveAll(Iterable<StaffPermission> entries) { return repository.saveAll(entries); }
}
