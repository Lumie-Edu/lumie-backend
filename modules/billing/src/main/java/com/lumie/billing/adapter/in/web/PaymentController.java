package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.ConfirmPaymentRequest;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.service.PaymentService;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String CONFIRM_SCOPE = "POST /v1/payments/confirm";

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/confirm")
    public ResponseEntity<InvoiceResponse> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        AuthorizationGuard.requireOwner();
        return idempotencyService.executeOnce(
                idempotencyKey,
                CONFIRM_SCOPE,
                request,
                () -> ResponseEntity.ok(paymentService.confirmPayment(request)));
    }

    @GetMapping("/history")
    @AllowedSortFields({"createdAt", "amount", "status"})
    public ResponseEntity<PageResponse<InvoiceResponse>> getPaymentHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(paymentService.getPaymentHistory(tenantId, pageable));
    }

    @GetMapping("/history/{tenantSlug}")
    @AllowedSortFields({"createdAt", "amount", "status"})
    public ResponseEntity<PageResponse<InvoiceResponse>> getPaymentHistoryBySlug(
            @PathVariable String tenantSlug,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AuthorizationGuard.requireOwner();
        return ResponseEntity.ok(paymentService.getPaymentHistoryBySlug(tenantSlug, pageable));
    }
}
