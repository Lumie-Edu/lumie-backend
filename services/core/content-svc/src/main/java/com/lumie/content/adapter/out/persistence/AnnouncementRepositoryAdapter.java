package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Announcement;
import com.lumie.content.domain.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AnnouncementRepositoryAdapter implements AnnouncementRepository {

    private final JpaAnnouncementRepository jpaRepository;

    @Override
    public Announcement save(Announcement announcement) {
        return jpaRepository.save(announcement);
    }

    @Override
    public Optional<Announcement> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Announcement> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<Announcement> findAllOrderByPinnedDescCreatedAtDesc(Pageable pageable) {
        return jpaRepository.findAllOrderByPinnedDescCreatedAtDesc(pageable);
    }

    @Override
    public List<Announcement> findByIsPinnedTrue() {
        return jpaRepository.findByIsPinnedTrue();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
