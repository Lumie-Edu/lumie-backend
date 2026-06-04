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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomepageQueryServiceTest {

    @Mock
    private HomepageConfigPersistencePort homepageConfigPersistencePort;

    @Mock
    private TenantLookupPort tenantLookupPort;

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private HomepageQueryService.Tx tx;

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

        HomepageConfigResponse result = homepageQueryService.getCurrent();

        assertThat(result.templateId()).isEqualTo(TemplateId.SOLO_INSTRUCTOR);
        assertThat(result.published()).isTrue();
    }

    @Test
    @DisplayName("홈페이지 설정이 없을 때 기본(미게시) config 반환")
    void getCurrent_returnsDefault_whenNoConfig() {
        when(homepageConfigPersistencePort.find()).thenReturn(Optional.empty());

        HomepageConfigResponse result = homepageQueryService.getCurrent();

        assertThat(result).isNotNull();
        assertThat(result.templateId()).isEqualTo(TemplateId.SOLO_INSTRUCTOR);
        assertThat(result.published()).isFalse();
        assertThat(result.updatedAt()).isNull();
    }

    @Test
    @DisplayName("공개 조회 - 테넌트를 찾으면 RLS 컨텍스트(tenantId 포함)를 세팅하고 Tx에서 조회")
    void getPublicByCustomId_tenantFound_setsTenantIdAndDelegatesToTx() {
        when(tenantLookupPort.findByCustomId("my-academy"))
                .thenReturn(Optional.of(new TenantInfo("my-academy", 1L)));
        AtomicReference<Long> tenantIdDuringTx = new AtomicReference<>();
        when(tx.findCurrent()).thenAnswer(inv -> {
            tenantIdDuringTx.set(TenantContextHolder.getTenantId());
            return Optional.of(HomepageConfigResponse.from(buildPublishedConfig(), objectMapper));
        });

        Optional<HomepageConfigResponse> result = homepageQueryService.getPublicByCustomId("my-academy");

        assertThat(result).isPresent();
        assertThat(result.get().published()).isTrue();
        // RLS filters on app.tenant_id — tenantId MUST be bound while Tx runs
        // (setTenant alone leaves the tenant's rows invisible).
        assertThat(tenantIdDuringTx.get()).isEqualTo(1L);
    }

    @Test
    @DisplayName("공개 조회 - 존재하지 않는 customId이면 빈 Optional, Tx 미호출")
    void getPublicByCustomId_unknownCustomId_returnsEmpty() {
        when(tenantLookupPort.findByCustomId("unknown")).thenReturn(Optional.empty());

        Optional<HomepageConfigResponse> result = homepageQueryService.getPublicByCustomId("unknown");

        assertThat(result).isEmpty();
        verify(tx, never()).findCurrent();
    }

    @Test
    @DisplayName("getPublicByCustomId - 이전 테넌트 컨텍스트가 호출 후 복원된다")
    void getPublicByCustomId_restoresPreviousTenantContext() {
        // Pre-existing tenant in context (simulates a request already scoped to "owner-tenant")
        TenantContextHolder.setTenant("owner-tenant");
        when(tenantLookupPort.findByCustomId("other-academy"))
                .thenReturn(Optional.of(new TenantInfo("other-academy", 2L)));
        when(tx.findCurrent()).thenReturn(Optional.empty());

        homepageQueryService.getPublicByCustomId("other-academy");

        // withinContext must restore the previous context after the call —
        // both slug and tenantId (prior context had no tenantId → null).
        assertThat(TenantContextHolder.getTenant()).isEqualTo("owner-tenant");
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Nested
    @DisplayName("Tx.findCurrent — 현재 설정 조회 + RLS 트랜잭션 경계")
    class TxTest {

        private final HomepageConfigPersistencePort port = mock(HomepageConfigPersistencePort.class);
        private final HomepageQueryService.Tx txBean = new HomepageQueryService.Tx(port, new ObjectMapper());

        @Test
        @DisplayName("저장된 설정이면 반환")
        void savedConfig_returnsConfig() {
            when(port.find()).thenReturn(Optional.of(buildPublishedConfig()));

            assertThat(txBean.findCurrent()).isPresent();
        }

        @Test
        @DisplayName("published=false여도 저장된 설정이면 반환")
        void unpublished_returnsConfig() {
            when(port.find()).thenReturn(Optional.of(buildUnpublishedConfig()));

            assertThat(txBean.findCurrent()).isPresent();
        }

        @Test
        @DisplayName("설정이 없으면 빈 Optional")
        void noConfig_returnsEmpty() {
            when(port.find()).thenReturn(Optional.empty());

            assertThat(txBean.findCurrent()).isEmpty();
        }
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
