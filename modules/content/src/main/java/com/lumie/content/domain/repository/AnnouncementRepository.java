package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC")
    Page<Announcement> findAllOrderByImportantDescCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.isItAssetAnnouncement = :isAsset ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC")
    Page<Announcement> findByIsItAssetAnnouncementOrderByImportantDescCreatedAtDesc(Boolean isAsset, Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE NOT EXISTS (
                SELECT 1 FROM AnnouncementClassTarget target
                WHERE target.announcementId = a.id
            )
            ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC
            """)
    Page<Announcement> findVisibleWithoutClassTargets(Pageable pageable);

    @Query("""
            SELECT DISTINCT a FROM Announcement a
            WHERE (
                NOT EXISTS (
                    SELECT 1 FROM AnnouncementClassTarget target
                    WHERE target.announcementId = a.id
                )
                OR EXISTS (
                    SELECT 1 FROM AnnouncementClassTarget target
                    WHERE target.announcementId = a.id
                      AND target.classId IN :classIds
                )
            )
            ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC
            """)
    Page<Announcement> findVisibleByClassTargets(@Param("classIds") List<Long> classIds, Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.isItAssetAnnouncement = :isAsset
              AND NOT EXISTS (
                  SELECT 1 FROM AnnouncementClassTarget target
                  WHERE target.announcementId = a.id
              )
            ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC
            """)
    Page<Announcement> findVisibleWithoutClassTargetsByIsAsset(@Param("isAsset") Boolean isAsset, Pageable pageable);

    @Query("""
            SELECT DISTINCT a FROM Announcement a
            WHERE a.isItAssetAnnouncement = :isAsset
              AND (
                  NOT EXISTS (
                      SELECT 1 FROM AnnouncementClassTarget target
                      WHERE target.announcementId = a.id
                  )
                  OR EXISTS (
                      SELECT 1 FROM AnnouncementClassTarget target
                      WHERE target.announcementId = a.id
                        AND target.classId IN :classIds
                  )
              )
            ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC
            """)
    Page<Announcement> findVisibleByClassTargetsAndIsAsset(
            @Param("isAsset") Boolean isAsset,
            @Param("classIds") List<Long> classIds,
            Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.isItImportantAnnouncement = true
              AND NOT EXISTS (
                  SELECT 1 FROM AnnouncementClassTarget target
                  WHERE target.announcementId = a.id
              )
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findImportantVisibleWithoutClassTargets();

    @Query("""
            SELECT DISTINCT a FROM Announcement a
            WHERE a.isItImportantAnnouncement = true
              AND (
                  NOT EXISTS (
                      SELECT 1 FROM AnnouncementClassTarget target
                      WHERE target.announcementId = a.id
                  )
                  OR EXISTS (
                      SELECT 1 FROM AnnouncementClassTarget target
                      WHERE target.announcementId = a.id
                        AND target.classId IN :classIds
                  )
              )
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findImportantVisibleByClassTargets(@Param("classIds") List<Long> classIds);

    List<Announcement> findByIsItImportantAnnouncementTrue();
}
