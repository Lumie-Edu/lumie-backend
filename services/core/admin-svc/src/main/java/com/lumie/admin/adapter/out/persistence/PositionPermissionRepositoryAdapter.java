package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.PositionPermission;
import com.lumie.admin.domain.repository.PositionPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PositionPermissionRepositoryAdapter implements PositionPermissionRepository {

    private final JpaPositionPermissionRepository jpaRepository;

    @Override
    public List<PositionPermission> findByPositionId(Long positionId) {
        return jpaRepository.findByPositionId(positionId);
    }

    @Override
    public void deleteByPositionId(Long positionId) {
        jpaRepository.deleteByPositionId(positionId);
    }

    @Override
    public void saveAll(List<PositionPermission> permissions) {
        jpaRepository.saveAll(permissions);
    }
}
