package com.lumie.billing.adapter.in.web;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.lumie.billing.application.dto.request.RegisterBillingKeyRequest;
import com.lumie.billing.application.dto.response.BillingKeyResponse;
import com.lumie.billing.application.service.BillingKeyCommandService;
import com.lumie.billing.application.service.BillingKeyQueryService;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/billing-keys")
@RequiredArgsConstructor
public class BillingKeyController {

    private static final String REGISTER_SCOPE = "POST /v1/billing-keys";
    private static final String REVOKE_SCOPE = "DELETE /v1/billing-keys/{billingKeyId}";

    private final BillingKeyCommandService billingKeyCommandService;
    private final BillingKeyQueryService billingKeyQueryService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<BillingKeyResponse> registerBillingKey(
            @Valid @RequestBody RegisterBillingKeyRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return idempotencyService.executeOnce(
                idempotencyKey,
                REGISTER_SCOPE,
                request,
                () -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(billingKeyCommandService.registerBillingKey(tenantId, request, idempotencyKey)));
    }

    @GetMapping
    public ResponseEntity<List<BillingKeyResponse>> getMyBillingKeys() {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(billingKeyQueryService.getAllBillingKeys(tenantId));
    }

    @GetMapping("/active")
    public ResponseEntity<BillingKeyResponse> getActiveBillingKey() {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(billingKeyQueryService.getActiveBillingKey(tenantId));
    }

    @DeleteMapping("/{billingKeyId}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> revokeBillingKey(
            @PathVariable Long billingKeyId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return idempotencyService.executeOnce(
                idempotencyKey,
                REVOKE_SCOPE,
                Map.of("billingKeyId", billingKeyId),
                () -> {
                    billingKeyCommandService.revokeBillingKey(tenantId, billingKeyId);
                    return ResponseEntity.noContent().build();
                });
    }
}
