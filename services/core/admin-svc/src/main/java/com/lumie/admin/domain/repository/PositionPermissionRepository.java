package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.PositionPermission;

import java.util.List;

public interface PositionPermissionRepository {
    List<PositionPermission> findByPositionId(Long positionId);
    void deleteByPositionId(Long positionId);
    void saveAll(List<PositionPermission> permissions);
}
