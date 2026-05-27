package com.lumie.tuition.adapter.in.web;

import com.lumie.tuition.application.dto.request.TossWebhookPayload;
import com.lumie.tuition.application.port.out.TuitionPaymentGatewayPort;
import com.lumie.tuition.application.service.TuitionWebhookApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal webhook endpoint for Toss Payments events (tuition domain).
 *
 * <p>This endpoint has no X-Tenant-Slug — the orderId encodes the invoiceId,
 * which is used to look up the tenant context before processing.
 *
 * <p>Idempotency: duplicate webhooks for an already-captured payment are
 * silently accepted (200 OK) — see TuitionWebhookApplicationService.
 *
 * <p>Responsibilities of this controller: signature verification + parse + delegate.
 * All business logic (tenant context, repo lookups, confirmPaymentFromWebhook) lives
 * in {@link TuitionWebhookApplicationService}.
 */
@Slf4j
@RestController
@RequestMapping("/internal/webhooks/toss/tuition")
@RequiredArgsConstructor
public class TossTuitionWebhookController {

    private final TuitionPaymentGatewayPort paymentGatewayPort;
    private final TuitionWebhookApplicationService webhookApplicationService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Toss-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        // Step 1: verify webhook signature (no tenant context needed — platform-level verification)
        TuitionPaymentGatewayPort.WebhookVerificationResult verificationResult =
                paymentGatewayPort.verifyWebhook(signature, rawBody);

        if (!verificationResult.valid()) {
            log.warn("Invalid Toss tuition webhook signature");
            return ResponseEntity.status(401).build();
        }

        TossWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, TossWebhookPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse Toss tuition webhook payload", e);
            return ResponseEntity.badRequest().build();
        }

        log.info("Received Toss tuition webhook: eventType={}, orderId={}",
                payload.eventType(), payload.data() != null ? payload.data().orderId() : "unknown");

        if (!payload.isPaymentDone() && !payload.isPaymentCanceled()) {
            log.debug("Unhandled Toss webhook eventType={} — returning 200", payload.eventType());
            return ResponseEntity.ok().build();
        }

        // Step 2: delegate all business logic (tenant resolution, DB lookups, state transitions)
        webhookApplicationService.handlePaymentWebhook(payload);

        return ResponseEntity.ok().build();
    }
}
