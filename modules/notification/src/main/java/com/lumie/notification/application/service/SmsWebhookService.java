package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SmsWebhookService {

    private final SmsProperties properties;
    private final TenantService tenantService;
    private final SmsDispatchService dispatchService;

    public void verifySecretHash(String receivedSecretHash) {
        String expectedSecret = properties.solapi().webhookToken();
        if (expectedSecret == null || expectedSecret.isBlank()
                || receivedSecretHash == null || receivedSecretHash.isBlank()
                || !MessageDigest.isEqual(
                        sha1Hex(expectedSecret).getBytes(StandardCharsets.UTF_8),
                        receivedSecretHash.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
                )) {
            throw new BusinessException(SmsErrorCode.WEBHOOK_UNAUTHORIZED);
        }
    }

    public Optional<SmsMessageResponse> applySolapiReport(String tenantSlug, Long messageId, int recipientIndex,
                                                          String providerMessageId, String providerGroupId,
                                                          String statusCode, String statusMessage) {
        if (tenantSlug == null || tenantSlug.isBlank()
                || providerMessageId == null || providerMessageId.isBlank()
                || messageId == null || recipientIndex < 0) {
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

    private String sha1Hex(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 digest is unavailable", e);
        }
    }
}
