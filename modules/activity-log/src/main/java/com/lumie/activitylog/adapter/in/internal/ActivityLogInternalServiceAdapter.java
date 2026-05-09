package com.lumie.activitylog.adapter.in.internal;

import com.lumie.activitylog.application.service.ActivityLogCommandService;
import com.lumie.activitylog.application.service.ActivityLogContext;
import com.lumie.internal.activitylog.ActivityLogInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityLogInternalServiceAdapter implements ActivityLogInternalService {

    private final ActivityLogCommandService activityLogCommandService;

    @Override
    public void log(Long actorId, String actorName, String actorRole,
                    String action, String entityType, Long entityId,
                    String entityName, String description) {
        activityLogCommandService.log(actorId, actorName, actorRole, action, entityType, entityId, entityName, description);
        ActivityLogContext.markLogged();
    }
}
