package com.lumie.homepage.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;
import com.lumie.homepage.application.port.out.HomepageConfigPersistencePort;
import com.lumie.homepage.application.port.out.TenantLookupPort;
import com.lumie.homepage.application.port.out.TenantLookupPort.TenantInfo;
import com.lumie.homepage.domain.entity.HomepageConfig;
import com.lumie.homepage.domain.vo.TemplateId;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomepageQueryServiceTest {

    @Mock
    private HomepageConfigPersistencePort homepageConfigPersistencePort;

    @Mock
    private TenantLookupPort tenantLookupPort;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private HomepageQueryService homepageQueryService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("현재 테넌트의 홈페이지 설정 조회 성공")
    void getCurrent_returnsConfig() {
        HomepageConfig config = buildPublishedConfig();
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(config));

        Optional<HomepageConfigResponse> result = homepageQueryService.getCurrent();

        assertThat(result).isPresent();
        assertThat(result.get().templateId()).isEqualTo(TemplateId.SOLO_INSTRUCTOR);
        assertThat(result.get().published()).isTrue();
    }

    @Test
    @DisplayName("홈페이지 설정이 없을 때 빈 Optional 반환")
    void getCurrent_returnsEmpty() {
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.empty());

        Optional<HomepageConfigResponse> result = homepageQueryService.getCurrent();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공개 조회 - published=true이면 설정 반환")
    void getPublicByCustomId_publishedConfig_returnsConfig() {
        when(tenantLookupPort.findByCustomId("my-academy"))
                .thenReturn(Optional.of(new TenantInfo("my-academy")));
        HomepageConfig config = buildPublishedConfig();
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(config));

        Optional<HomepageConfigResponse> result = homepageQueryService.getPublicByCustomId("my-academy");

        assertThat(result).isPresent();
        assertThat(result.get().published()).isTrue();
    }

    @Test
    @DisplayName("공개 조회 - published=false이면 빈 Optional 반환")
    void getPublicByCustomId_unpublishedConfig_returnsEmpty() {
        when(tenantLookupPort.findByCustomId("my-academy"))
                .thenReturn(Optional.of(new TenantInfo("my-academy")));
        // Config with meaningful content but unpublished
        HomepageConfig config = buildUnpublishedConfig();
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(config));

        Optional<HomepageConfigResponse> result = homepageQueryService.getPublicByCustomId("my-academy");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공개 조회 - 존재하지 않는 customId이면 빈 Optional 반환")
    void getPublicByCustomId_unknownCustomId_returnsEmpty() {
        when(tenantLookupPort.findByCustomId("unknown")).thenReturn(Optional.empty());

        Optional<HomepageConfigResponse> result = homepageQueryService.getPublicByCustomId("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPublicByCustomId - 이전 테넌트 컨텍스트가 호출 후 복원된다")
    void getPublicByCustomId_restoresPreviousTenantContext() {
        // Arrange: pre-existing tenant in context (simulates a request already scoped to "owner-tenant")
        TenantContextHolder.setTenant("owner-tenant");

        when(tenantLookupPort.findByCustomId("other-academy"))
                .thenReturn(Optional.of(new TenantInfo("other-academy")));
        HomepageConfig config = buildUnpublishedConfig();
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(config));

        homepageQueryService.getPublicByCustomId("other-academy");

        // The previous tenant must be restored in the finally block
        assertThat(TenantContextHolder.getTenant()).isEqualTo("owner-tenant");
    }

    @Test
    @DisplayName("getPublicByCustomId_returns_empty_when_unpublished")
    void getPublicByCustomId_returns_empty_when_unpublished() {
        when(tenantLookupPort.findByCustomId("my-academy"))
                .thenReturn(Optional.of(new TenantInfo("my-academy")));
        HomepageConfig config = buildUnpublishedConfig();
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.of(config));

        Optional<HomepageConfigResponse> result = homepageQueryService.getPublicByCustomId("my-academy");

        assertThat(result).isEmpty();
    }

    // --- helpers ---

    private HomepageConfig buildPublishedConfig() {
        HomepageConfig config = HomepageConfig.create(
                TemplateId.SOLO_INSTRUCTOR, 1,
                "{\"hero\":{\"enabled\":true}}"
        );
        ReflectionTestUtils.setField(config, "id", 1L);
        config.publish();
        return config;
    }

    private HomepageConfig buildUnpublishedConfig() {
        HomepageConfig config = HomepageConfig.create(
                TemplateId.SOLO_INSTRUCTOR, 1,
                "{\"hero\":{\"enabled\":true}}"
        );
        ReflectionTestUtils.setField(config, "id", 1L);
        // published defaults to false — no explicit unpublish needed
        return config;
    }
}
