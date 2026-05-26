package com.lumie.content.application.service;

import com.lumie.content.application.dto.request.CreateAnnouncementRequest;
import com.lumie.content.application.dto.request.UpdateAnnouncementRequest;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.common.exception.BusinessException;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.repository.AnnouncementRepository;
import com.lumie.file.api.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementCommandService {

    private final AnnouncementRepository announcementRepository;
    private final FileService fileService;

    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request) {
        log.info("Creating announcement: {}", request.announcementTitle());

        Announcement announcement = Announcement.create(
                request.authorId(),
                request.announcementTitle(),
                request.announcementContent(),
                request.isItAssetAnnouncement(),
                request.isItImportantAnnouncement()
        );

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement created with id: {}", saved.getId());

        return AnnouncementResponse.from(saved);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, UpdateAnnouncementRequest request) {
        log.info("Updating announcement: {}", id);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND));

        announcement.update(
                request.announcementTitle(),
                request.announcementContent(),
                request.isItAssetAnnouncement(),
                request.isItImportantAnnouncement()
        );

        if (request.fileIdsToDelete() != null && !request.fileIdsToDelete().isEmpty()) {
            fileService.deleteFilesByIds(request.fileIdsToDelete());
        }

        Announcement updated = announcementRepository.save(announcement);
        log.info("Announcement updated: {}", updated.getId());

        return AnnouncementResponse.from(updated);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        log.info("Deleting announcement: {}", id);

        if (!announcementRepository.existsById(id)) {
            throw new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }

        fileService.deleteFilesByEntity("announcement", id);
        announcementRepository.deleteById(id);
        log.info("Announcement deleted: {}", id);
    }
}
