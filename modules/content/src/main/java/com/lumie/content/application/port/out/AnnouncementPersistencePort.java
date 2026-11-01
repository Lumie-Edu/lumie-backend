package com.lumie.content.application.port.out;

import com.lumie.content.domain.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface AnnouncementPersistencePort {
    Page<Announcement> findAllOrderByImportantDescCreatedAtDesc(Pageable pageable);
    Page<Announcement> findByIsItAssetAnnouncementOrderByImportantDescCreatedAtDesc(Boolean isAsset, Pageable pageable);
    Page<Announcement> findAcademyWide(Pageable pageable);
    Page<Announcement> findVisibleToClasses(List<Long> classIds, Pageable pageable);
    Page<Announcement> findAcademyWideByIsAsset(Boolean isAsset, Pageable pageable);
    Page<Announcement> findVisibleToClassesByIsAsset(Boolean isAsset, List<Long> classIds, Pageable pageable);
    List<Announcement> findImportantAcademyWide();
    List<Announcement> findImportantVisibleToClasses(List<Long> classIds);
    List<Announcement> findByIsItImportantAnnouncementTrue();
    Optional<Announcement> findById(Long id);
    boolean existsById(Long id);
    Announcement save(Announcement announcement);
    void flush();
    void deleteById(Long id);
}
