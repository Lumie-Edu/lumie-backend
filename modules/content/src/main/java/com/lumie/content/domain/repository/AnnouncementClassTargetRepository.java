package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.AnnouncementClassTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AnnouncementClassTargetRepository extends JpaRepository<AnnouncementClassTarget, Long> {

    List<AnnouncementClassTarget> findByAnnouncementId(Long announcementId);

    List<AnnouncementClassTarget> findByAnnouncementIdIn(Collection<Long> announcementIds);

    void deleteByAnnouncementId(Long announcementId);
}
