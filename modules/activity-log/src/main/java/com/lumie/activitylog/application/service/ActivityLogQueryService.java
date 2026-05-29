package com.lumie.activitylog.application.service;

import com.lumie.activitylog.application.dto.request.ActivityLogFilterRequest;
import com.lumie.activitylog.application.dto.response.ActivityLogResponse;
import com.lumie.activitylog.domain.entity.ActivityLog;
import com.lumie.activitylog.domain.exception.ActivityLogErrorCode;
import com.lumie.activitylog.domain.repository.ActivityLogRepository;
import com.lumie.common.api.CursorResponse;
import com.lumie.common.auth.EntityType;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogQueryService {

    private final ActivityLogRepository activityLogRepository;

    public CursorResponse<ActivityLogResponse> getLogs(ActivityLogFilterRequest filter,
                                                       String cursor,
                                                       int limit) {
        Role requesterRole = UserContextHolder.getUserRole();
        List<Role> visibleRoles = getVisibleRoles(requesterRole);
        if (visibleRoles.isEmpty()) {
            throw new BusinessException(ActivityLogErrorCode.INSUFFICIENT_ROLE);
        }

        Cursor decoded = decodeCursor(cursor);
        Specification<ActivityLog> spec = buildSpecification(filter, visibleRoles, decoded);

        // Fetch limit+1 to detect the existence of a next page without paying for COUNT(*).
        List<ActivityLog> rows = activityLogRepository.findBy(spec, q -> q
                .sortBy(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
                .limit(limit + 1)
                .all());

        String nextCursor = null;
        if (rows.size() > limit) {
            ActivityLog last = rows.get(limit - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
            // Detach from the fluent-query-backed list; subList alone would
            // return a live view of the original.
            rows = new ArrayList<>(rows.subList(0, limit));
        }

        List<ActivityLogResponse> items = rows.stream().map(ActivityLogResponse::from).toList();
        return CursorResponse.of(items, nextCursor, limit);
    }

    private Specification<ActivityLog> buildSpecification(ActivityLogFilterRequest filter,
                                                          List<Role> visibleRoles,
                                                          Cursor cursor) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(root.get("actorRole").in(visibleRoles));

            if (filter != null && filter.actorId() != null) {
                predicates.add(cb.equal(root.get("actorId"), filter.actorId()));
            }

            if (filter != null && filter.entityType() != null && !filter.entityType().isBlank()) {
                EntityType parsed;
                try {
                    parsed = EntityType.valueOf(filter.entityType());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(ActivityLogErrorCode.INVALID_ENTITY_TYPE, e);
                }
                predicates.add(cb.equal(root.get("entityType"), parsed));
            }

            if (filter != null && filter.startDate() != null) {
                Instant from = filter.startDate().atStartOfDay(ZoneOffset.UTC).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (filter != null && filter.endDate() != null) {
                Instant to = filter.endDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }

            if (filter != null && filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("actorName")), pattern));
            }

            // Keyset predicate: ordering is (createdAt DESC, id DESC); seek past the cursor.
            if (cursor != null) {
                Predicate olderTs = cb.lessThan(root.get("createdAt"), cursor.createdAt());
                Predicate sameTsLowerId = cb.and(
                        cb.equal(root.get("createdAt"), cursor.createdAt()),
                        cb.lessThan(root.get("id"), cursor.id()));
                predicates.add(cb.or(olderTs, sameTsLowerId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<Role> getVisibleRoles(Role role) {
        return switch (role) {
            case OWNER -> List.of(Role.MANAGER, Role.INSTRUCTOR, Role.STUDENT);
            case MANAGER -> List.of(Role.INSTRUCTOR, Role.STUDENT);
            default -> List.of();
        };
    }

    private String encodeCursor(Instant createdAt, Long id) {
        String raw = createdAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf(':');
            if (sep <= 0) {
                throw new BusinessException(ActivityLogErrorCode.INVALID_CURSOR);
            }
            long ts = Long.parseLong(raw.substring(0, sep));
            long id = Long.parseLong(raw.substring(sep + 1));
            return new Cursor(Instant.ofEpochMilli(ts), id);
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            // Covers both Base64 decode failures and NumberFormatException
            // (NFE extends IAE) when ts/id portions are not valid longs.
            throw new BusinessException(ActivityLogErrorCode.INVALID_CURSOR, e);
        }
    }

    private record Cursor(Instant createdAt, Long id) {
    }
}
