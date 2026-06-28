package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SmsWebhookService {

    private final SmsProperties properties;
    private final TenantService tenantService;
    private final SmsDispatchService dispatchService;

    public void verifyToken(String token) {
        String expected = properties.solapi().webhookToken();
        if (expected == null || expected.isBlank() || token == null || !expected.equals(token)) {
            throw new BusinessException(SmsErrorCode.WEBHOOK_UNAUTHORIZED);
        }
    }

    public Optional<SmsMessageResponse> applySolapiReport(String tenantSlug, Long messageId, int recipientIndex,
                                                          String providerMessageId, String providerGroupId,
                                                          String statusCode, String statusMessage) {
        if (tenantSlug == null || tenantSlug.isBlank() || messageId == null || recipientIndex < 0) {
            return Optional.empty();
        }
        TenantService.TenantData tenant = tenantService.getTenantBySlug(tenantSlug)
                .orElseThrow(() -> new BusinessException(SmsErrorCode.WEBHOOK_IGNORED, "Webhook tenant not found"));
        return Optional.of(TenantContextHolder.withinContext(tenant.slug(), tenant.id(), () ->
                dispatchService.applyWebhookReport(new SmsDispatchService.SolapiWebhookReport(
                        messageId,
                        recipientIndex,
                        providerMessageId,
                        providerGroupId,
                        statusCode,
                        statusMessage
                ))
        ));
    }
}
