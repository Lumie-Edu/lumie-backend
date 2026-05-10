package com.lumie.homepage.application.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.homepage.domain.entity.HomepageConfig;
import com.lumie.homepage.domain.vo.TemplateId;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

@Builder
public record HomepageConfigResponse(
        Long id,
        TemplateId templateId,
        int templateVersion,
        boolean published,
        JsonNode sections,
        Instant updatedAt
) {

    private static final Logger log = LoggerFactory.getLogger(HomepageConfigResponse.class);

    public static HomepageConfigResponse from(HomepageConfig config, ObjectMapper objectMapper) {
        return HomepageConfigResponse.builder()
                .id(config.getId())
                .templateId(config.getTemplateId())
                .templateVersion(config.getTemplateVersion())
                .published(config.isPublished())
                .sections(parseJson(objectMapper, config.getId(), "sections", config.getSections()))
                .updatedAt(Timestamps.utcInstant(config.getUpdatedAt()))
                .build();
    }

    private static JsonNode parseJson(ObjectMapper objectMapper, Long configId, String field, String raw) {
        if (raw == null) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("Failed to parse JSONB for homepage config {} field {}: {}", configId, field, e.getMessage());
            return objectMapper.nullNode();
        }
    }
}
