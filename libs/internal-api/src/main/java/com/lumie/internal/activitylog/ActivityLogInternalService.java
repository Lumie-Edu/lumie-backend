package com.lumie.internal.activitylog;

public interface ActivityLogInternalService {

    void log(Long actorId, String actorName, String actorRole,
             String action, String entityType, Long entityId,
             String entityName, String description);
}
