package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);

    @Query("SELECT r.userId FROM AnnouncementRead r WHERE r.announcement.id = :announcementId ORDER BY r.readAt DESC")
    List<Long> findUserIdsByAnnouncementId(@Param("announcementId") Long announcementId);
}
