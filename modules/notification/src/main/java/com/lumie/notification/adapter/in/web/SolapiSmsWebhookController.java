package com.lumie.notification.adapter.in.web;

import com.lumie.common.exception.BusinessException;
import com.lumie.notification.application.service.SmsWebhookService;
import com.lumie.notification.domain.exception.SmsErrorCode;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Hidden
@Slf4j
@RestController
@RequestMapping("/v1/sms/solapi/webhook")
@RequiredArgsConstructor
public class SolapiSmsWebhookController {

    private final SmsWebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Solapi-Secret", required = false) String secretHash,
            @RequestBody List<SolapiWebhookEvent> events) {
        webhookService.verifySecretHash(secretHash);
        for (SolapiWebhookEvent event : events) {
            try {
                webhookService.applySolapiReport(
                        value(event.customFields(), "lumieTenant"),
                        parseLong(value(event.customFields(), "lumieSmsId")),
                        parseInt(value(event.customFields(), "lumieRecipient")),
                        event.messageId(),
                        event.groupId(),
                        event.statusCode(),
                        event.statusMessage()
                );
            } catch (BusinessException e) {
                if (e.getErrorCode() != SmsErrorCode.WEBHOOK_IGNORED) {
                    throw e;
                }
                log.info("Ignored SOLAPI webhook event: groupId={}, messageId={}, reason={}",
                        event.groupId(), event.messageId(), e.getMessage());
            }
        }
        return ResponseEntity.ok().build();
    }

    private String value(Map<String, String> customFields, String key) {
        return customFields == null ? null : customFields.get(key);
    }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String value) {
        try {
            return value == null ? -1 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public record SolapiWebhookEvent(
            String messageId,
            String groupId,
            String statusCode,
            String statusMessage,
            Map<String, String> customFields
    ) {
    }
}
