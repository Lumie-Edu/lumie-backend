package com.lumie.homepage.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.homepage.application.dto.request.PublishHomepageRequest;
import com.lumie.homepage.application.dto.request.UpdateHomepageConfigRequest;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;
import com.lumie.homepage.application.port.out.HomepageConfigPersistencePort;
import com.lumie.homepage.domain.entity.HomepageConfig;
import com.lumie.common.exception.BusinessException;
import com.lumie.homepage.domain.exception.HomepageErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HomepageCommandService {

    private final HomepageConfigPersistencePort homepageConfigPersistencePort;
    private final ObjectMapper objectMapper;

    public HomepageConfigResponse update(UpdateHomepageConfigRequest request) {
        log.info("Upserting homepage config: templateId={}, version={}", request.templateId(), request.templateVersion());

        String sectionsJson = serializeJson(request.sections());

        HomepageConfig config = homepageConfigPersistencePort.find()
                .orElseGet(() -> HomepageConfig.create(
                        request.templateId(),
                        request.templateVersion(),
                        sectionsJson
                ));

        if (config.getId() != null) {
            config.update(request.templateId(), request.templateVersion(), sectionsJson);
        }

        HomepageConfig saved = homepageConfigPersistencePort.save(config);
        log.info("Homepage config saved: id={}", saved.getId());
        return HomepageConfigResponse.from(saved, objectMapper);
    }

    public HomepageConfigResponse publish(PublishHomepageRequest request) {
        log.info("Toggling homepage publish: published={}", request.published());

        HomepageConfig config = homepageConfigPersistencePort.find()
                .orElseThrow(() -> new BusinessException(HomepageErrorCode.HOMEPAGE_CONFIG_NOT_FOUND));

        if (request.published()) {
            config.publish();
        } else {
            config.unpublish();
        }
        HomepageConfig saved = homepageConfigPersistencePort.save(config);
        log.info("Homepage config publish state updated: id={}, published={}", saved.getId(), saved.isPublished());
        return HomepageConfigResponse.from(saved, objectMapper);
    }

    private String serializeJson(com.fasterxml.jackson.databind.JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new BusinessException(HomepageErrorCode.JSON_SERIALIZATION_FAILED, e);
        }
    }
}
