package com.lumie.notification.adapter.in.web;

import com.lumie.notification.application.service.SmsWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Hidden
@RestController
@RequestMapping("/v1/sms/solapi/webhook")
@RequiredArgsConstructor
public class SolapiSmsWebhookController {

    private final SmsWebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Solapi-Webhook-Token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody List<SolapiWebhookEvent> events) {
        webhookService.verifyToken(resolveToken(token, authorization));
        for (SolapiWebhookEvent event : events) {
            webhookService.applySolapiReport(
                    value(event.customFields(), "lumieTenant"),
                    parseLong(value(event.customFields(), "lumieSmsId")),
                    parseInt(value(event.customFields(), "lumieRecipient")),
                    event.messageId(),
                    event.groupId(),
                    event.statusCode(),
                    event.statusMessage()
            );
        }
        return ResponseEntity.ok().build();
    }

    private String resolveToken(String token, String authorization) {
        if (token != null && !token.isBlank()) {
            return token;
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length());
        }
        return null;
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
