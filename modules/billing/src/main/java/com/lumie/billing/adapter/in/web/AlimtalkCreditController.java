package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.ConfigureAutoRechargeRequest;
import com.lumie.billing.application.dto.request.RechargeAlimtalkCreditRequest;
import com.lumie.billing.application.dto.response.AlimtalkCreditResponse;
import com.lumie.billing.application.service.AlimtalkCreditCommandService;
import com.lumie.billing.application.service.AlimtalkCreditQueryService;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/alimtalk/credits")
@RequiredArgsConstructor
public class AlimtalkCreditController {

    private static final String RECHARGE_SCOPE = "POST /v1/alimtalk/credits/recharge";
    private static final String CONFIGURE_AUTO_RECHARGE_SCOPE = "PUT /v1/alimtalk/credits/auto-recharge";
    private static final String DISABLE_AUTO_RECHARGE_SCOPE = "DELETE /v1/alimtalk/credits/auto-recharge";

    private final AlimtalkCreditCommandService alimtalkCreditCommandService;
    private final AlimtalkCreditQueryService alimtalkCreditQueryService;
    private final IdempotencyService idempotencyService;

    @GetMapping
    public ResponseEntity<AlimtalkCreditResponse> getMyCreditBalance() {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(alimtalkCreditQueryService.getCreditBalance(tenantId));
    }

    @PostMapping("/recharge")
    public ResponseEntity<AlimtalkCreditResponse> recharge(
            @Valid @RequestBody RechargeAlimtalkCreditRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return idempotencyService.executeOnce(
                idempotencyKey,
                RECHARGE_SCOPE,
                request,
                () -> ResponseEntity.ok(alimtalkCreditCommandService.recharge(tenantId, request)));
    }

    @PutMapping("/auto-recharge")
    public ResponseEntity<AlimtalkCreditResponse> configureAutoRecharge(
            @Valid @RequestBody ConfigureAutoRechargeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return idempotencyService.executeOnce(
                idempotencyKey,
                CONFIGURE_AUTO_RECHARGE_SCOPE,
                request,
                () -> ResponseEntity.ok(alimtalkCreditCommandService.enableAutoRecharge(tenantId, request)));
    }

    @DeleteMapping("/auto-recharge")
    public ResponseEntity<AlimtalkCreditResponse> disableAutoRecharge(
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        AuthorizationGuard.requireOwner();
        Long tenantId = TenantContextHolder.getTenantId();
        return idempotencyService.executeOnce(
                idempotencyKey,
                DISABLE_AUTO_RECHARGE_SCOPE,
                Map.of("tenantId", tenantId, "op", "disable_auto_recharge"),
                () -> ResponseEntity.ok(alimtalkCreditCommandService.disableAutoRecharge(tenantId)));
    }
}
