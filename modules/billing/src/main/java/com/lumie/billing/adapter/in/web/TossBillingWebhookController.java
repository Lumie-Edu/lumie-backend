package com.lumie.billing.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.application.dto.request.TossBillingWebhookPayload;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Toss billing/subscription webhook receiver.
 * Endpoint: POST /internal/webhooks/toss/billing
 *
 * Raw body is captured as String to allow HMAC-SHA256 signature verification
 * before deserializing the payload.
 */
@Slf4j
@RestController
@RequestMapping("/internal/webhooks/toss")
@RequiredArgsConstructor
public class TossBillingWebhookController {

    private final PaymentGatewayPort paymentGatewayPort;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/billing", consumes = "application/json")
    public ResponseEntity<Void> handleBillingWebhook(
            @RequestHeader(value = "Toss-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (signature != null) {
            PaymentGatewayPort.WebhookVerificationResult verification =
                    paymentGatewayPort.verifyWebhook(signature, rawBody);
            if (!verification.valid()) {
                log.warn("Toss billing webhook signature invalid");
                throw new BillingException(BillingErrorCode.TOSS_WEBHOOK_SIGNATURE_INVALID,
                        "Invalid Toss webhook signature");
            }
        }

        TossBillingWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, TossBillingWebhookPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse Toss billing webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        log.info("Toss billing webhook received: eventType={}", payload.eventType());

        // TODO (Task 5): dispatch to service based on eventType
        // e.g. if payload.isPaymentDone() → invoiceCommandService.confirmFromBillingWebhook(...)
        //      if payload.isBillingKeyIssued() → billingKeyCommandService.handleKeyIssuedEvent(...)

        return ResponseEntity.ok().build();
    }
}
