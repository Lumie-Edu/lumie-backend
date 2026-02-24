package com.lumie.ai.adapter.out.external;

import com.lumie.ai.application.port.out.ContentServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentServiceClient implements ContentServicePort {

    private final InternalServiceClient internalServiceClient;

    @Value("${internal.content-svc.url:http://content-svc.lumie-content.svc:8080}")
    private String contentSvcUrl;

    @Override
    public String createAnnouncement(Map<String, Object> arguments) {
        Map<String, Object> body = new HashMap<>();
        body.put("announcementTitle", arguments.get("title"));
        body.put("announcementContent", arguments.get("content"));
        body.put("isItImportantAnnouncement", arguments.getOrDefault("isImportant", false));
        body.put("isItAssetAnnouncement", arguments.getOrDefault("isAsset", false));

        Map<String, Object> result = internalServiceClient.post(
                contentSvcUrl, "/api/v1/announcements", body);

        return "Announcement created successfully: " + result.get("announcementTitle");
    }
}
