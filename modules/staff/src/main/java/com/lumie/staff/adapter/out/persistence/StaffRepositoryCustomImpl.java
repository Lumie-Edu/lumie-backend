package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.repository.StaffRepositoryCustom;
import com.lumie.staff.domain.vo.StaffFilter;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Component;

// Spring Data's default fragment-impl scan walks the package of the
// fragment interface (com.lumie.staff.domain.repository). Hexagonal layout
// puts the impl under adapter/out/persistence, so the default scan misses
// it and the repository proxy falls back to query derivation — which then
// breaks on findAllByFilter ("Staff has no 'filter' property"). Marking the
// class as a Spring component makes the regular component-scan find it
// and Spring Data binds it to StaffRepositoryCustom by type + naming.
@Component
public class StaffRepositoryCustomImpl implements StaffRepositoryCustom {

    private final SimpleJpaRepository<Staff, Long> base;

    public StaffRepositoryCustomImpl(EntityManager entityManager) {
        this.base = new SimpleJpaRepository<>(
                JpaEntityInformationSupport.getEntityInformation(Staff.class, entityManager),
                entityManager);
    }

    @Override
    public Page<Staff> findAllByFilter(StaffFilter filter, Pageable pageable) {
        return base.findAll(StaffSpecification.withFilters(filter), pageable);
    }
}
