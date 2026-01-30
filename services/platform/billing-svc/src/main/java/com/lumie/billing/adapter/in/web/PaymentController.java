package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.ConfirmPaymentRequest;
import com.lumie.billing.application.dto.request.TossWebhookPayload;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.dto.response.PaymentResponse;
import com.lumie.billing.application.port.in.ConfirmPaymentUseCase;
import com.lumie.billing.application.port.in.GetPaymentHistoryUseCase;
import com.lumie.billing.application.port.in.HandleWebhookUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ConfirmPaymentUseCase confirmPaymentUseCase;
    private final HandleWebhookUseCase handleWebhookUseCase;
    private final GetPaymentHistoryUseCase getPaymentHistoryUseCase;

    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request) {
        PaymentResponse response = confirmPaymentUseCase.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody TossWebhookPayload payload) {
        log.info("Received webhook: eventType={}", payload.eventType());
        handleWebhookUseCase.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/{tenantSlug}")
    public ResponseEntity<List<InvoiceResponse>> getPaymentHistory(@PathVariable String tenantSlug) {
        List<InvoiceResponse> history = getPaymentHistoryUseCase.getPaymentHistory(tenantSlug);
        return ResponseEntity.ok(history);
    }
}
