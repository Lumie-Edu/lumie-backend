package com.lumie.content.application.port.out;

import java.util.List;

public interface AnnouncementReadPersistencePort {
    int insertIfAbsent(Long tenantId, Long announcementId, Long userId);
    List<Long> findUserIdsByAnnouncementId(Long announcementId);
}
