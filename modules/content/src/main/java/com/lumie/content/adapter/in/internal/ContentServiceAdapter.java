package com.lumie.content.adapter.in.internal;

import com.lumie.content.application.dto.request.CreateAnnouncementRequest;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.application.service.AnnouncementCommandService;
import com.lumie.content.application.service.DeletionReadinessService;
import com.lumie.content.application.service.AnnouncementQueryService;
import com.lumie.content.application.service.QnaQueryService;
import com.lumie.content.api.ContentService;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentServiceAdapter implements ContentService {

    private final AnnouncementCommandService announcementCommandService;
    private final AnnouncementQueryService announcementQueryService;
    private final QnaQueryService qnaQueryService;
    private final DeletionReadinessService deletionReadinessService;

    @Override
    public String createAnnouncement(Long authorId, String title, String content,
                                     boolean isImportant, boolean isAsset) {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                authorId, title, content, isAsset, isImportant, null, null);
        AnnouncementResponse response = announcementCommandService.createAnnouncement(request);
        return "Announcement created successfully: " + response.announcementTitle();
    }

    @Override
    public boolean canUserViewAnnouncement(Long announcementId, String role, Long userId) {
        return announcementQueryService.canUserViewAnnouncement(announcementId, role, userId);
    }

    @Override
    public boolean canUserViewQna(Long qnaId, String role, Long userId) {
        return qnaQueryService.canUserViewQna(qnaId, role, userId);
    }

    @Override
    public boolean hasActiveOrUpcomingReservationsForStudent(String tenantSlug, Long studentId) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                deletionReadinessService.hasActiveOrUpcomingReservationsForStudent(studentId)
        );
    }

    @Override
    public boolean hasUpcomingSchedulesForUser(String tenantSlug, Long userId) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                deletionReadinessService.hasUpcomingSchedulesForUser(userId)
        );
    }
}
