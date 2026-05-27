package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.RegisterBillingKeyRequest;
import com.lumie.billing.application.dto.response.BillingKeyResponse;
import com.lumie.billing.application.service.BillingKeyCommandService;
import com.lumie.billing.application.service.BillingKeyQueryService;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/billing-keys")
@RequiredArgsConstructor
public class BillingKeyController {

    private final BillingKeyCommandService billingKeyCommandService;
    private final BillingKeyQueryService billingKeyQueryService;

    @PostMapping
    public ResponseEntity<BillingKeyResponse> registerBillingKey(
            @Valid @RequestBody RegisterBillingKeyRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long tenantId = TenantContextHolder.getTenantId();
        BillingKeyResponse response = billingKeyCommandService.registerBillingKey(
                tenantId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BillingKeyResponse>> getMyBillingKeys() {
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(billingKeyQueryService.getAllBillingKeys(tenantId));
    }

    @GetMapping("/active")
    public ResponseEntity<BillingKeyResponse> getActiveBillingKey() {
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(billingKeyQueryService.getActiveBillingKey(tenantId));
    }

    @DeleteMapping("/{billingKeyId}")
    public ResponseEntity<Void> revokeBillingKey(@PathVariable Long billingKeyId) {
        Long tenantId = TenantContextHolder.getTenantId();
        billingKeyCommandService.revokeBillingKey(tenantId, billingKeyId);
        return ResponseEntity.noContent().build();
    }
}
