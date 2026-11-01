package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.application.port.out.PermissionPersistencePort;
import com.lumie.staff.domain.entity.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PermissionPersistenceAdapter implements PermissionPersistencePort {
    private final PermissionJpaRepository repository;
    @Override public List<Permission> findAll() { return repository.findAll(); }
    @Override public List<Permission> findByCategory(String category) { return repository.findByCategory(category); }
    @Override public Set<Permission> findByIdIn(Set<Long> ids) { return repository.findByIdIn(ids); }
    @Override public List<String> findDistinctCategories() { return repository.findDistinctCategories(); }
}
