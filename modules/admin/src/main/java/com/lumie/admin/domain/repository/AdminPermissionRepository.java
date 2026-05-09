package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.AdminPermission;
import com.lumie.admin.domain.entity.AdminPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, AdminPermissionId> {

    List<AdminPermission> findByAdminId(Long adminId);

    void deleteByAdminId(Long adminId);
}
