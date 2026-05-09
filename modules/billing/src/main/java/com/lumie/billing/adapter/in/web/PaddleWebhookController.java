package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.PaddleWebhookPayload;
import com.lumie.billing.application.service.PaddleWebhookService;
import com.lumie.billing.infrastructure.config.PaddleConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@RestController
@RequestMapping("/v1/payments/paddle")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "paddle.enabled", havingValue = "true")
public class PaddleWebhookController {

    private static final Duration MAX_TIMESTAMP_DRIFT = Duration.ofMinutes(5);

    private final PaddleWebhookService paddleWebhookService;
    private final PaddleConfig paddleConfig;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("Paddle-Signature") String signature,
            @RequestBody String rawBody) {

        if (!verifySignature(rawBody, signature)) {
            log.warn("Invalid Paddle webhook signature");
            return ResponseEntity.status(401).build();
        }

        try {
            PaddleWebhookPayload payload = objectMapper.readValue(rawBody, PaddleWebhookPayload.class);
            paddleWebhookService.handleWebhook(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to process Paddle webhook", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean verifySignature(String payload, String signatureHeader) {
        try {
            String[] parts = signatureHeader.split(";");
            String ts = null;
            String h1 = null;
            for (String part : parts) {
                if (part.startsWith("ts=")) ts = part.substring(3);
                if (part.startsWith("h1=")) h1 = part.substring(3);
            }
            if (ts == null || h1 == null) return false;

            long timestamp = Long.parseLong(ts);
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - timestamp) > MAX_TIMESTAMP_DRIFT.toSeconds()) {
                log.warn("Paddle webhook timestamp too old: ts={}, now={}", ts, now);
                return false;
            }

            String signedPayload = ts + ":" + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    paddleConfig.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    h1.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException | NumberFormatException e) {
            log.error("Webhook signature verification error", e);
            return false;
        }
    }
}
