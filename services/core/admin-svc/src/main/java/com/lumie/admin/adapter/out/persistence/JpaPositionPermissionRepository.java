package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.PositionPermission;
import com.lumie.admin.domain.entity.PositionPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPositionPermissionRepository extends JpaRepository<PositionPermission, PositionPermissionId> {
    List<PositionPermission> findByPositionId(Long positionId);
    void deleteByPositionId(Long positionId);
}
