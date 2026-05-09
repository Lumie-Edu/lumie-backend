package com.lumie.homepage.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumie.homepage.application.dto.request.PublishHomepageRequest;
import com.lumie.homepage.application.dto.request.UpdateHomepageConfigRequest;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;
import com.lumie.homepage.application.port.out.HomepageConfigPersistencePort;
import com.lumie.homepage.domain.entity.HomepageConfig;
import com.lumie.homepage.domain.exception.HomepageConfigNotFoundException;
import com.lumie.homepage.domain.exception.HomepageNotReadyToPublishException;
import com.lumie.homepage.domain.vo.TemplateId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomepageCommandServiceTest {

    @Mock
    private HomepageConfigPersistencePort homepageConfigPersistencePort;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private HomepageCommandService homepageCommandService;

    private ObjectNode sectionsNode;

    @BeforeEach
    void setUp() {
        sectionsNode = objectMapper.createObjectNode();
        sectionsNode.put("hero", true);
    }

    @Test
    @DisplayName("설정이 없을 때 update() 호출 시 새로 생성")
    void update_createsNewConfig_whenNoneExists() {
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.empty());
        when(homepageConfigPersistencePort.save(any())).thenAnswer(inv -> {
            HomepageConfig c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 1L);
            return c;
        });

        UpdateHomepageConfigRequest request = UpdateHomepageConfigRequest.builder()
                .templateId(TemplateId.SOLO_INSTRUCTOR)
                .templateVersion(1)
                .sections(sectionsNode)
                .build();

        HomepageConfigResponse response = homepageCommandService.update(request);

        assertThat(response.templateId()).isEqualTo(TemplateId.SOLO_INSTRUCTOR);
        assertThat(response.templateVersion()).isEqualTo(1);
        assertThat(response.published()).isFalse();
    }

    @Test
    @DisplayName("설정이 있을 때 update() 호출 시 기존 설정 업데이트")
    void update_updatesExistingConfig() {
        HomepageConfig existing = HomepageConfig.create(TemplateId.SOLO_INSTRUCTOR, 1, "{}");
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(existing));
        when(homepageConfigPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateHomepageConfigRequest request = UpdateHomepageConfigRequest.builder()
                .templateId(TemplateId.SOLO_INSTRUCTOR)
                .templateVersion(2)
                .sections(sectionsNode)
                .build();

        HomepageConfigResponse response = homepageCommandService.update(request);

        assertThat(response.templateId()).isEqualTo(TemplateId.SOLO_INSTRUCTOR);
        assertThat(response.templateVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("publish_succeeds_with_complete_config - published=true 설정")
    void publish_succeeds_with_complete_config() {
        HomepageConfig config = HomepageConfig.create(
                TemplateId.SOLO_INSTRUCTOR, 1,
                "{\"hero\":{\"enabled\":true}}"
        );
        ReflectionTestUtils.setField(config, "id", 1L);
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(config));
        when(homepageConfigPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HomepageConfigResponse response = homepageCommandService.publish(new PublishHomepageRequest(true));

        assertThat(response.published()).isTrue();
    }

    @Test
    @DisplayName("publish_fails_when_blank - sections가 빈 JSON이면 예외")
    void publish_fails_when_blank() {
        HomepageConfig config = HomepageConfig.create(
                TemplateId.SOLO_INSTRUCTOR, 1, "{}"
        );
        ReflectionTestUtils.setField(config, "id", 1L);
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> homepageCommandService.publish(new PublishHomepageRequest(true)))
                .isInstanceOf(HomepageNotReadyToPublishException.class);
    }

    @Test
    @DisplayName("unpublish_succeeds - published=false 설정")
    void unpublish_succeeds() {
        HomepageConfig config = HomepageConfig.create(
                TemplateId.SOLO_INSTRUCTOR, 1,
                "{\"hero\":{\"enabled\":true}}"
        );
        ReflectionTestUtils.setField(config, "id", 1L);
        config.publish();
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(config));
        when(homepageConfigPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HomepageConfigResponse response = homepageCommandService.publish(new PublishHomepageRequest(false));

        assertThat(response.published()).isFalse();
    }

    @Test
    @DisplayName("publish() 설정이 없을 때 예외")
    void publish_throwsWhenConfigNotFound() {
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> homepageCommandService.publish(new PublishHomepageRequest(true)))
                .isInstanceOf(HomepageConfigNotFoundException.class);
    }
}
