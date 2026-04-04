package com.lumie.ai.adapter.out.internal;

import com.lumie.ai.application.port.out.ContentServicePort;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.internal.content.ContentInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentInternalAdapter implements ContentServicePort {

    private final ContentInternalService contentInternalService;

    @Override
    public String createAnnouncement(Map<String, Object> arguments) {
        Long authorId = UserContextHolder.getRequiredUserId();
        String title = (String) arguments.get("title");
        String content = (String) arguments.get("content");
        boolean isImportant = (boolean) arguments.getOrDefault("isImportant", false);
        boolean isAsset = (boolean) arguments.getOrDefault("isAsset", false);

        log.debug("Creating announcement via internal API: title={}, authorId={}", title, authorId);
        return contentInternalService.createAnnouncement(authorId, title, content, isImportant, isAsset);
    }
}
