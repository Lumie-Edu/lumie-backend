package com.lumie.activitylog.application.service;

import com.lumie.activitylog.application.dto.ActivityLogFilterRequest;
import com.lumie.activitylog.application.dto.ActivityLogResponse;
import com.lumie.activitylog.domain.entity.ActivityLog;
import com.lumie.activitylog.domain.exception.ActivityLogErrorCode;
import com.lumie.activitylog.domain.repository.ActivityLogRepository;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogQueryService {

    private final ActivityLogRepository activityLogRepository;

    public Page<ActivityLogResponse> getLogs(ActivityLogFilterRequest filter, Pageable pageable) {
        String requesterRole = UserContextHolder.getUserRole();
        List<String> visibleRoles = getVisibleRoles(requesterRole);

        if (visibleRoles.isEmpty()) {
            throw new BusinessException(ActivityLogErrorCode.INSUFFICIENT_ROLE);
        }

        Specification<ActivityLog> spec = buildSpecification(filter, visibleRoles);
        return activityLogRepository.findAll(spec, pageable).map(ActivityLogResponse::from);
    }

    private Specification<ActivityLog> buildSpecification(ActivityLogFilterRequest filter, List<String> visibleRoles) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(root.get("actorRole").in(visibleRoles));

            if (filter != null && filter.actorId() != null) {
                predicates.add(cb.equal(root.get("actorId"), filter.actorId()));
            }

            if (filter != null && filter.entityType() != null && !filter.entityType().isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), filter.entityType()));
            }

            if (filter != null && filter.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        filter.startDate().atStartOfDay()));
            }

            if (filter != null && filter.endDate() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"),
                        filter.endDate().plusDays(1).atStartOfDay()));
            }

            if (filter != null && filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("actorName")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<String> getVisibleRoles(String role) {
        return switch (role) {
            case "OWNER" -> List.of("MANAGER", "INSTRUCTOR");
            case "MANAGER" -> List.of("INSTRUCTOR");
            default -> List.of();
        };
    }
}
