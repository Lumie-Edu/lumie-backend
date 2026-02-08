package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Permission;
import com.lumie.admin.domain.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepository {
    private final JpaPermissionRepository jpaPermissionRepository;

    @Override
    public List<Permission> findAll() {
        return jpaPermissionRepository.findAll();
    }

    @Override
    public List<Permission> findByCategory(String category) {
        return jpaPermissionRepository.findByCategory(category);
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return jpaPermissionRepository.findById(id);
    }

    @Override
    public Set<Permission> findByIdIn(Set<Long> ids) {
        return jpaPermissionRepository.findByIdIn(ids);
    }

    @Override
    public List<String> findDistinctCategories() {
        return jpaPermissionRepository.findDistinctCategories();
    }
}
