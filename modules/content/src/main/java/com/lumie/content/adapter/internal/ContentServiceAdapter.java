package com.lumie.content.adapter.internal;

import com.lumie.content.application.dto.request.CreateAnnouncementRequest;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.application.service.AnnouncementCommandService;
import com.lumie.internal.content.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentServiceAdapter implements ContentService {

    private final AnnouncementCommandService announcementCommandService;

    @Override
    public String createAnnouncement(Long authorId, String title, String content,
                                     boolean isImportant, boolean isAsset) {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                authorId, title, content, isAsset, isImportant);
        AnnouncementResponse response = announcementCommandService.createAnnouncement(request);
        return "Announcement created successfully: " + response.announcementTitle();
    }
}
