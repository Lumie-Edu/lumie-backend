package com.lumie.student.adapter.out.persistence;

import com.lumie.student.application.port.out.StudentPersistencePort;
import com.lumie.student.domain.entity.Student;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudentPersistenceAdapter implements StudentPersistencePort {

    private final StudentJpaRepository repository;

    @Override public Student save(Student student) { return repository.save(student); }
    @Override public List<Student> saveAll(Iterable<Student> students) { return repository.saveAll(students); }
    // Deletion suspends its service transaction; keep these lookups RLS-bound on the primary.
    @Override
    @Transactional
    public Optional<Student> findById(Long id) { return repository.findById(id); }

    @Override
    @Transactional
    public List<Student> findAllById(Iterable<Long> ids) { return repository.findAllById(ids); }
    @Override public Page<Student> findAll(Pageable pageable) { return repository.findAll(pageable); }
    @Override public Optional<Student> findByUserId(Long userId) { return repository.findByUserId(userId); }
    @Override public List<Student> findByUserIdIn(Collection<Long> userIds) { return repository.findByUserIdIn(userIds); }
    @Override public Optional<Student> findByUserLoginId(String loginId) { return repository.findByUserLoginId(loginId); }
    @Override public Optional<Student> findByCanonicalPhone(String phone) { return repository.findByCanonicalPhone(phone); }

    @Override
    public Page<Student> findByIsActive(Boolean isActive, Pageable pageable) {
        return repository.findByIsActive(isActive, pageable);
    }

    @Override public long countByIsActive(Boolean isActive) { return repository.countByIsActive(isActive); }
    @Override public long countByCreatedAtBetween(Instant from, Instant to) { return repository.countByCreatedAtBetween(from, to); }
    @Override public long countByCreatedAtBefore(Instant cutoff) { return repository.countByCreatedAtBefore(cutoff); }
    @Override public List<Object[]> countMonthlyEnrollmentsSince(Instant from) { return repository.countMonthlyEnrollmentsSince(from); }
    @Override public long countByIsActiveAndUpdatedAtBetween(Boolean active, Instant from, Instant to) { return repository.countByIsActiveAndUpdatedAtBetween(active, from, to); }
    @Override public long countByIsActiveAndUpdatedAtBefore(Boolean active, Instant cutoff) { return repository.countByIsActiveAndUpdatedAtBefore(active, cutoff); }
    @Override public List<Object[]> countMonthlyDropoutsSince(Instant from) { return repository.countMonthlyDropoutsSince(from); }
    @Override public boolean existsByUserLoginId(String loginId) { return repository.existsByUserLoginId(loginId); }
    @Override public boolean existsByCanonicalPhone(String phone) { return repository.existsByCanonicalPhone(phone); }
    @Override public boolean existsByCanonicalPhoneExcluding(String phone, Long excludeId) { return repository.existsByCanonicalPhoneExcluding(phone, excludeId); }

    @Override
    public Page<Student> search(Boolean isActive, String search, Collection<Long> includedIds,
                                Collection<Long> excludedIds, Pageable pageable) {
        Specification<Student> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isActive != null) predicates.add(cb.equal(root.get("isActive"), isActive));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("canonicalName")), pattern),
                        cb.like(cb.lower(root.get("studentHighschool")), pattern),
                        cb.like(root.get("canonicalPhone"), "%" + search + "%"),
                        cb.like(root.get("parentPhone"), "%" + search + "%"),
                        cb.like(cb.toString(root.get("studentBirthYear")), "%" + search + "%")));
            }
            if (includedIds != null && !includedIds.isEmpty()) predicates.add(root.get("id").in(includedIds));
            if (excludedIds != null && !excludedIds.isEmpty()) predicates.add(cb.not(root.get("id").in(excludedIds)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        if (pageable.isUnpaged() || pageable.getSort().isUnsorted()) return repository.findAll(specification, pageable);
        var orders = pageable.getSort().stream().map(order -> order.withProperty(switch (order.getProperty()) {
            case "name" -> "canonicalName";
            case "phone" -> "canonicalPhone";
            default -> order.getProperty();
        })).toList();
        return repository.findAll(specification, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders)));
    }
}
