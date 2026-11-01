package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.application.port.out.StaffPersistencePort;
import com.lumie.staff.domain.entity.Staff;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffPersistenceAdapter implements StaffPersistencePort {
    private final StaffJpaRepository repository;
    @Override public Optional<Staff> findById(Long id) { return repository.findById(id); }
    @Override public Optional<Staff> findByUserId(Long userId) { return repository.findByUserId(userId); }
    @Override public Optional<Staff> findByUserLoginId(String userLoginId) { return repository.findByUserLoginId(userLoginId); }
    @Override public boolean existsByUserLoginId(String userLoginId) { return repository.existsByUserLoginId(userLoginId); }
    @Override public boolean existsByCanonicalPhone(String phone) { return repository.existsByCanonicalPhone(phone); }
    @Override public boolean existsByCanonicalPhoneAndIdNot(String phone, Long excludeId) {
        return repository.existsByCanonicalPhoneAndIdNot(phone, excludeId);
    }
    @Override public long countByIsActive(Boolean isActive) { return repository.countByIsActive(isActive); }
    @Override public List<Object[]> countGroupedByRoleAndIsActive(Boolean isActive) {
        return repository.countGroupedByRoleAndIsActive(isActive);
    }
    @Override public Page<Staff> search(Boolean isActive, String search, Pageable pageable) {
        Specification<Staff> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isActive != null) predicates.add(cb.equal(root.get("isActive"), isActive));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("canonicalName")), pattern),
                        cb.like(cb.lower(root.get("userLoginId")), pattern),
                        cb.like(cb.lower(root.get("canonicalEmail")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        if (pageable.isUnpaged() || pageable.getSort().isUnsorted()) return repository.findAll(spec, pageable);
        var orders = pageable.getSort().stream()
                .map(order -> order.withProperty("name".equals(order.getProperty()) ? "canonicalName" : order.getProperty()))
                .toList();
        return repository.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders)));
    }
    @Override public Staff save(Staff staff) { return repository.save(staff); }
    @Override public void delete(Staff staff) { repository.delete(staff); }
}
