package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    @Modifying
    @Query(value = """
            insert into announcement_reads (tenant_id, announcement_id, user_id, read_at)
            values (:tenantId, :announcementId, :userId, current_timestamp)
            on conflict (announcement_id, user_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("tenantId") Long tenantId,
                       @Param("announcementId") Long announcementId,
                       @Param("userId") Long userId);

    @Query("SELECT r.userId FROM AnnouncementRead r WHERE r.announcement.id = :announcementId ORDER BY r.readAt DESC")
    List<Long> findUserIdsByAnnouncementId(@Param("announcementId") Long announcementId);
}
