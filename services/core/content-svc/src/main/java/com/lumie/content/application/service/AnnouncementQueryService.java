package com.lumie.content.application.service;

import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnnouncementQueryService {

    private final AnnouncementRepository announcementRepository;

    public Page<AnnouncementResponse> listAnnouncements(Boolean isAsset, Pageable pageable) {
        log.debug("Listing announcements with pagination, isAsset={}", isAsset);

        if (isAsset != null) {
            return announcementRepository.findByIsItAssetAnnouncementOrderByImportantDescCreatedAtDesc(isAsset, pageable)
                    .map(AnnouncementResponse::from);
        }
        return announcementRepository.findAllOrderByImportantDescCreatedAtDesc(pageable)
                .map(AnnouncementResponse::from);
    }

    public List<AnnouncementResponse> listImportantAnnouncements() {
        log.debug("Listing important announcements");

        return announcementRepository.findByIsItImportantAnnouncementTrue().stream()
                .map(AnnouncementResponse::from)
                .toList();
    }

    public AnnouncementResponse getAnnouncement(Long id) {
        log.debug("Getting announcement: {}", id);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND));

        return AnnouncementResponse.from(announcement);
    }
}
