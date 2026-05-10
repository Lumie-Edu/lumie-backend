package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.ConfirmPaymentRequest;
import com.lumie.billing.application.dto.request.TossWebhookPayload;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.dto.response.PaymentResponse;
import com.lumie.billing.application.service.PaymentService;
import com.lumie.common.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String CONFIRM_SCOPE = "POST /v1/payments/confirm";

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    CONFIRM_SCOPE,
                    request,
                    () -> ResponseEntity.ok(paymentService.confirmPayment(request))
            );
        }
        PaymentResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody TossWebhookPayload payload) {
        log.info("Received webhook: eventType={}", payload.eventType());
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/{tenantSlug}")
    public ResponseEntity<List<InvoiceResponse>> getPaymentHistory(@PathVariable String tenantSlug) {
        List<InvoiceResponse> history = paymentService.getPaymentHistory(tenantSlug);
        return ResponseEntity.ok(history);
    }
}
