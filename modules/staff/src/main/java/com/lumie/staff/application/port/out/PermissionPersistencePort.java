package com.lumie.staff.application.port.out;

import com.lumie.staff.domain.entity.Permission;

import java.util.List;
import java.util.Set;

public interface PermissionPersistencePort {
    List<Permission> findAll();
    List<Permission> findByCategory(String category);
    Set<Permission> findByIdIn(Set<Long> ids);
    List<String> findDistinctCategories();
}
