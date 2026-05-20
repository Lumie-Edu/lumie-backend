package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.repository.StaffRepositoryCustom;
import com.lumie.staff.domain.vo.StaffFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

public class StaffRepositoryImpl implements StaffRepositoryCustom {

    private final SimpleJpaRepository<Staff, Long> base;

    public StaffRepositoryImpl(EntityManager entityManager) {
        this.base = new SimpleJpaRepository<>(
                JpaEntityInformationSupport.getEntityInformation(Staff.class, entityManager),
                entityManager);
    }

    @Override
    public Page<Staff> findAllByFilter(StaffFilter filter, Pageable pageable) {
        return base.findAll(StaffSpecification.withFilters(filter), pageable);
    }
}
