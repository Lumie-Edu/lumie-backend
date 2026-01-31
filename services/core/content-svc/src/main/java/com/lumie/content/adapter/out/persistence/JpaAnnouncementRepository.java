package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaAnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a ORDER BY a.isImportant DESC, a.createdAt DESC")
    Page<Announcement> findAllOrderByImportantDescCreatedAtDesc(Pageable pageable);

    List<Announcement> findByIsImportantTrue();
}
