package com.lumie.content.api;


public interface ContentService {

    String createAnnouncement(Long authorId, String title, String content,
                              boolean isImportant, boolean isAsset);
}
