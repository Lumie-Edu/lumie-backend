package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.repository.AdminRepositoryCustom;
import com.lumie.admin.domain.vo.AdminFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

public class AdminRepositoryImpl implements AdminRepositoryCustom {

    private final SimpleJpaRepository<Admin, Long> base;

    public AdminRepositoryImpl(EntityManager entityManager) {
        this.base = new SimpleJpaRepository<>(
                JpaEntityInformationSupport.getEntityInformation(Admin.class, entityManager),
                entityManager);
    }

    @Override
    public Page<Admin> findAllByFilter(AdminFilter filter, Pageable pageable) {
        return base.findAll(AdminSpecification.withFilters(filter), pageable);
    }
}
