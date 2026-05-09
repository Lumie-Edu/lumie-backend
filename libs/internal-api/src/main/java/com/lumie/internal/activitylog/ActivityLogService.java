package com.lumie.internal.activitylog;

public interface ActivityLogService {

    void log(Long actorId, String actorName, String actorRole,
             String action, String entityType, Long entityId,
             String entityName, String description);
}
