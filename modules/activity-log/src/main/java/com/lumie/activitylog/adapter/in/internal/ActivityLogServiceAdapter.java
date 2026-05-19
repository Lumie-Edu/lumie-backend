package com.lumie.activitylog.adapter.in.internal;

import com.lumie.activitylog.application.service.ActivityLogCommandService;
import com.lumie.activitylog.api.ActivityLogService;
import com.lumie.common.activitylog.ActivityLogContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceAdapter implements ActivityLogService {

    private final ActivityLogCommandService activityLogCommandService;

    @Override
    public void log(Long actorId, String actorName, String actorRole,
                    String action, String entityType, Long entityId,
                    String entityName, String description) {
        activityLogCommandService.log(actorId, actorName, actorRole, action, entityType, entityId, entityName, description);
        ActivityLogContext.markLogged();
    }
}
