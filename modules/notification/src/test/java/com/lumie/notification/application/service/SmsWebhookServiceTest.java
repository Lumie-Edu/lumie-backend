package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsMessageType;
import com.lumie.notification.domain.vo.SmsStatus;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmsWebhookServiceTest {

    @Test
    void verifySecretHash_rejectsMissingOrInvalidSecretHash() {
        SmsWebhookService service = service(mock(TenantService.class), mock(SmsDispatchService.class));

        assertThatThrownBy(() -> service.verifySecretHash(null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.verifySecretHash("wrong"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void verifySecretHash_acceptsSolapiSha1SecretHeader() {
        SmsWebhookService service = service(mock(TenantService.class), mock(SmsDispatchService.class));

        service.verifySecretHash("ee977806d7286510da8b9a7492ba58e2484c0ecc");
    }

    @Test
    void applySolapiReport_restoresTenantContextAndDelegatesToDispatchService() {
        TenantService tenantService = mock(TenantService.class);
        SmsDispatchService dispatchService = mock(SmsDispatchService.class);
        SmsWebhookService service = service(tenantService, dispatchService);
        given(tenantService.getTenantBySlug("inst-test"))
                .willReturn(Optional.of(new TenantService.TenantData(
                        1L,
                        "inst-test",
                        "Test",
                        TenantStatus.ACTIVE,
                        "test",
                        null,
                        null,
                        null
                )));
        given(dispatchService.applyWebhookReport(any()))
                .willReturn(response());

        Optional<SmsMessageResponse> result = service.applySolapiReport(
                "inst-test",
                7L,
                0,
                "M_TEST_1",
                "G_TEST",
                "4000",
                "수신 완료"
        );

        assertThat(result).isPresent();
        verify(dispatchService).applyWebhookReport(any());
    }

    @Test
    void applySolapiReport_ignoresPayloadWithoutLumieMappingFields() {
        SmsWebhookService service = service(mock(TenantService.class), mock(SmsDispatchService.class));

        Optional<SmsMessageResponse> result = service.applySolapiReport(
                null,
                null,
                -1,
                "M_TEST_1",
                "G_TEST",
                "4000",
                "수신 완료"
        );

        assertThat(result).isEmpty();
    }

    private SmsWebhookService service(TenantService tenantService, SmsDispatchService dispatchService) {
        SmsProperties properties = new SmsProperties(
                "mock",
                "0212345678",
                50,
                3,
                new SmsProperties.Solapi("key", "secret", "https://api.solapi.com", "token")
        );
        return new SmsWebhookService(properties, tenantService, dispatchService);
    }

    private SmsMessageResponse response() {
        Instant now = Instant.now();
        return new SmsMessageResponse(
                7L,
                10L,
                RecipientType.GUARDIAN,
                List.of(),
                1,
                "제목",
                null,
                "내용",
                null,
                null,
                null,
                now,
                SmsMessageType.SMS,
                1,
                20,
                20,
                SmsStatus.SENT,
                1,
                0,
                now,
                now
        );
    }
}
