package com.lumie.content.api;


public interface ContentService {

    String createAnnouncement(Long authorId, String title, String content,
                              boolean isImportant, boolean isAsset);

    boolean canUserViewAnnouncement(Long announcementId, String role, Long userId);

    boolean canUserViewQna(Long qnaId, String role, Long userId);
}
