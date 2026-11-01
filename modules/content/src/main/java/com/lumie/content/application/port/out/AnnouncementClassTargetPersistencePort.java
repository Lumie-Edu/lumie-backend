package com.lumie.content.application.port.out;

import com.lumie.content.domain.entity.AnnouncementClassTarget;
import java.util.Collection;
import java.util.List;

public interface AnnouncementClassTargetPersistencePort {
    List<AnnouncementClassTarget> findByAnnouncementId(Long id);
    List<AnnouncementClassTarget> findByAnnouncementIdIn(Collection<Long> ids);
    void deleteByAnnouncementId(Long id);
    void flush();
    <S extends AnnouncementClassTarget> List<S> saveAll(Iterable<S> entities);
}
