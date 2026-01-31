package com.lumie.content.application.service;

import com.lumie.content.application.dto.request.CreateAnnouncementRequest;
import com.lumie.content.application.dto.request.UpdateAnnouncementRequest;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementCommandService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request) {
        log.info("Creating announcement: {}", request.title());

        Announcement announcement = Announcement.create(
                request.title(),
                request.content(),
                request.authorId(),
                request.authorName(),
                request.isImportant()
        );

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement created with id: {}", saved.getId());

        return AnnouncementResponse.from(saved);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, UpdateAnnouncementRequest request) {
        log.info("Updating announcement: {}", id);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND));

        announcement.update(request.title(), request.content(), request.isImportant());

        Announcement updated = announcementRepository.save(announcement);
        log.info("Announcement updated: {}", updated.getId());

        return AnnouncementResponse.from(updated);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        log.info("Deleting announcement: {}", id);

        if (!announcementRepository.existsById(id)) {
            throw new ContentException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }

        announcementRepository.deleteById(id);
        log.info("Announcement deleted: {}", id);
    }
}
