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
            WHERE a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.ALL
            ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC
            """)
    Page<Announcement> findAcademyWide(Pageable pageable);

    @Query("""
            SELECT DISTINCT a FROM Announcement a
            WHERE (
                a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.ALL
                OR (a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.TARGETED
                AND EXISTS (
                    SELECT 1 FROM AnnouncementClassTarget target
                    WHERE target.announcementId = a.id
                      AND target.classId IN :classIds
                ))
            )
            ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC
            """)
    Page<Announcement> findVisibleToClasses(@Param("classIds") List<Long> classIds, Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.isItAssetAnnouncement = :isAsset
              AND a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.ALL
            ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC
            """)
    Page<Announcement> findAcademyWideByIsAsset(@Param("isAsset") Boolean isAsset, Pageable pageable);

    @Query("""
            SELECT DISTINCT a FROM Announcement a
            WHERE a.isItAssetAnnouncement = :isAsset
              AND (
                  a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.ALL
                  OR (a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.TARGETED
                  AND EXISTS (
                      SELECT 1 FROM AnnouncementClassTarget target
                      WHERE target.announcementId = a.id
                        AND target.classId IN :classIds
                  ))
              )
            ORDER BY a.isItImportantAnnouncement DESC, a.createdAt DESC
            """)
    Page<Announcement> findVisibleToClassesByIsAsset(
            @Param("isAsset") Boolean isAsset,
            @Param("classIds") List<Long> classIds,
            Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.isItImportantAnnouncement = true
              AND a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.ALL
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findImportantAcademyWide();

    @Query("""
            SELECT DISTINCT a FROM Announcement a
            WHERE a.isItImportantAnnouncement = true
              AND (
                  a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.ALL
                  OR (a.audienceScope = com.lumie.content.domain.vo.AnnouncementAudienceScope.TARGETED
                  AND EXISTS (
                      SELECT 1 FROM AnnouncementClassTarget target
                      WHERE target.announcementId = a.id
                        AND target.classId IN :classIds
                  ))
              )
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findImportantVisibleToClasses(@Param("classIds") List<Long> classIds);

    List<Announcement> findByIsItImportantAnnouncementTrue();
}
