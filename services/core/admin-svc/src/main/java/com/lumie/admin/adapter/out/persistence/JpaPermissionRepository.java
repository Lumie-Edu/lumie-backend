package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface JpaPermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByCategory(String category);
    Set<Permission> findByIdIn(Set<Long> ids);

    @Query("SELECT DISTINCT p.category FROM Permission p ORDER BY p.category")
    List<String> findDistinctCategories();
}
