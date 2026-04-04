package com.lumie.internal.content;

import java.util.Map;

public interface ContentInternalService {

    String createAnnouncement(Long authorId, String title, String content,
                              boolean isImportant, boolean isAsset);
}
