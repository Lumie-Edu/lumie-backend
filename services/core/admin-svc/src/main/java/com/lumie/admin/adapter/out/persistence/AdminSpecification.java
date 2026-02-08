package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.application.dto.AdminFilterRequest;
import com.lumie.admin.domain.entity.Admin;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AdminSpecification {

    private AdminSpecification() {}

    public static Specification<Admin> withFilters(AdminFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.positionId() != null) {
                predicates.add(cb.equal(root.get("position").get("id"), filter.positionId()));
            }

            if (filter.academyId() != null) {
                predicates.add(cb.isMember(filter.academyId(), root.get("academyIds")));
            }

            if (filter.isActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.isActive()));
            }

            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("userLoginId")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
