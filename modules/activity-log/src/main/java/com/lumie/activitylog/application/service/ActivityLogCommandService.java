package com.lumie.activitylog.application.service;

import com.lumie.activitylog.domain.entity.ActivityLog;
import com.lumie.activitylog.domain.repository.ActivityLogRepository;
import com.lumie.common.auth.ActionType;
import com.lumie.common.auth.EntityType;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogCommandService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * String-typed entry point matching the {@code internal-api} contract.
     * Callers from in-process modules hand off {@code Role.name() / ActionType.name()}
     * etc., so {@code valueOf} is exact; only the {@code actorRole} path keeps
     * {@link Role#fromString} as a defensive fallback for any legacy alias
     * still flowing in from a JWT claim.
     */
    public void log(Long actorId, String actorName, String actorRole,
                    String action, String entityType, Long entityId,
                    String entityName, String description) {
        Role parsedActorRole = Role.fromString(actorRole);
        ActivityLog activityLog = ActivityLog.create(
                actorId, actorName, parsedActorRole,
                ActionType.valueOf(action), EntityType.valueOf(entityType),
                entityId, entityName, description
        );
        activityLogRepository.save(activityLog);
        Map<String, String> previousMdc = putActivityMdc(
                actorId, actorName, parsedActorRole.name(), action, entityType, entityId, entityName);
        try {
            log.info("Activity {} {} {} {}", action, entityType, entityId, actorId);
        } finally {
            restoreMdc(previousMdc);
        }
    }

    private Map<String, String> putActivityMdc(Long actorId, String actorName, String actorRole,
                                               String action, String entityType, Long entityId,
                                               String entityName) {
        Map<String, String> previous = new HashMap<>();
        putMdc(previous, "eventType", "activity");
        putMdc(previous, "outcome", "success");
        putMdc(previous, "tenantSlug", TenantContextHolder.getTenant());
        putMdc(previous, "tenantId", TenantContextHolder.getTenantId());
        putMdc(previous, "actorId", actorId);
        putMdc(previous, "actorName", actorName);
        putMdc(previous, "actorRole", actorRole);
        putMdc(previous, "action", action);
        putMdc(previous, "entityType", entityType);
        putMdc(previous, "entityId", entityId);
        putMdc(previous, "entityName", entityName);
        return previous;
    }

    private void putMdc(Map<String, String> previous, String key, Object value) {
        previous.put(key, MDC.get(key));
        if (value != null) {
            MDC.put(key, String.valueOf(value));
        } else {
            MDC.remove(key);
        }
    }

    private void restoreMdc(Map<String, String> previous) {
        previous.forEach((key, value) -> {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });
    }
}
