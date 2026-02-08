package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionRepository {
    List<Permission> findAll();
    List<Permission> findByCategory(String category);
    Optional<Permission> findById(Long id);
    Set<Permission> findByIdIn(Set<Long> ids);
    List<String> findDistinctCategories();
}
