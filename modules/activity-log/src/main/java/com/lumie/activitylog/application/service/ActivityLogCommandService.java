package com.lumie.activitylog.application.service;

import com.lumie.activitylog.domain.entity.ActivityLog;
import com.lumie.activitylog.domain.repository.ActivityLogRepository;
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

    public void log(Long actorId, String actorName, String actorRole,
                    String action, String entityType, Long entityId,
                    String entityName, String description) {
        ActivityLog activityLog = ActivityLog.create(
                actorId, actorName, actorRole, action, entityType, entityId, entityName, description
        );
        activityLogRepository.save(activityLog);
        log.info("[ACTIVITY] actor={} role={} action={} entity={} entityId={} entityName={} tenant={}",
                actorName, actorRole, action, entityType, entityId, entityName,
                TenantContextHolder.getRequiredTenant());
    }
}
