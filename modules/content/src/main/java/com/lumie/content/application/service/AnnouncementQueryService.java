package com.lumie.content.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.internal.admin.AdminService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnnouncementQueryService {

    private static final String DELETED_USER_NAME = "삭제된 유저";

    private final AnnouncementRepository announcementRepository;
    private final AdminService adminService;

    public Page<AnnouncementResponse> listAnnouncements(Boolean isAsset, Pageable pageable) {
        log.debug("Listing announcements with pagination, isAsset={}", isAsset);

        Page<Announcement> page = isAsset != null
                ? announcementRepository.findByIsItAssetAnnouncementOrderByImportantDescCreatedAtDesc(isAsset, pageable)
                : announcementRepository.findAllOrderByImportantDescCreatedAtDesc(pageable);

        Map<Long, String> authorNames = resolveAuthorNames(page.getContent());
        return page.map(a -> AnnouncementResponse.from(a, authorNameOrFallback(a.getAuthorId(), authorNames)));
    }

    public List<AnnouncementResponse> listImportantAnnouncements() {
        log.debug("Listing important announcements");

        List<Announcement> announcements = announcementRepository.findByIsItImportantAnnouncementTrue();
        Map<Long, String> authorNames = resolveAuthorNames(announcements);
        return announcements.stream()
                .map(a -> AnnouncementResponse.from(a, authorNameOrFallback(a.getAuthorId(), authorNames)))
                .toList();
    }

    public AnnouncementResponse getAnnouncement(Long id) {
        log.debug("Getting announcement: {}", id);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND));

        Map<Long, String> authorNames = resolveAuthorNames(List.of(announcement));
        return AnnouncementResponse.from(announcement, authorNameOrFallback(announcement.getAuthorId(), authorNames));
    }

    private Map<Long, String> resolveAuthorNames(List<Announcement> announcements) {
        List<Long> authorIds = announcements.stream()
                .map(Announcement::getAuthorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (authorIds.isEmpty()) {
            return Map.of();
        }

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return authorIds.stream()
                .flatMap(authorId -> adminService.getAdminByUserId(tenantSlug, authorId)
                        .map(admin -> Map.entry(authorId, admin.name()))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }

    private String authorNameOrFallback(Long authorId, Map<Long, String> authorNames) {
        if (authorId == null) {
            return DELETED_USER_NAME;
        }
        return authorNames.getOrDefault(authorId, DELETED_USER_NAME);
    }
}
