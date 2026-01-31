package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository {

    Announcement save(Announcement announcement);

    Optional<Announcement> findById(Long id);

    Page<Announcement> findAll(Pageable pageable);

    Page<Announcement> findAllOrderByImportantDescCreatedAtDesc(Pageable pageable);

    List<Announcement> findByIsImportantTrue();

    void deleteById(Long id);

    boolean existsById(Long id);
}
