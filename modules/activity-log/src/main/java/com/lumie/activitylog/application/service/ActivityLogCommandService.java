package com.lumie.activitylog.application.service;

import com.lumie.activitylog.domain.entity.ActivityLog;
import com.lumie.activitylog.domain.repository.ActivityLogRepository;
import com.lumie.common.auth.ActionType;
import com.lumie.common.auth.EntityType;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        ActivityLog activityLog = ActivityLog.create(
                actorId, actorName, Role.fromString(actorRole),
                ActionType.valueOf(action), EntityType.valueOf(entityType),
                entityId, entityName, description
        );
        activityLogRepository.save(activityLog);
        log.info("[ACTIVITY] actor={} role={} action={} entity={} entityId={} entityName={} tenant={}",
                actorName, actorRole, action, entityType, entityId, entityName,
                TenantContextHolder.getRequiredTenant());
    }
}
