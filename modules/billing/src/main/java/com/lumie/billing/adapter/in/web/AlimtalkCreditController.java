package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.ConfigureAutoRechargeRequest;
import com.lumie.billing.application.dto.request.RechargeAlimtalkCreditRequest;
import com.lumie.billing.application.dto.response.AlimtalkCreditResponse;
import com.lumie.billing.application.service.AlimtalkCreditCommandService;
import com.lumie.billing.application.service.AlimtalkCreditQueryService;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/alimtalk/credits")
@RequiredArgsConstructor
public class AlimtalkCreditController {

    private static final String RECHARGE_SCOPE = "POST /v1/alimtalk/credits/recharge";

    private final AlimtalkCreditCommandService alimtalkCreditCommandService;
    private final AlimtalkCreditQueryService alimtalkCreditQueryService;
    private final IdempotencyService idempotencyService;

    @GetMapping
    public ResponseEntity<AlimtalkCreditResponse> getMyCreditBalance() {
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(alimtalkCreditQueryService.getCreditBalance(tenantId));
    }

    @PostMapping("/recharge")
    public ResponseEntity<AlimtalkCreditResponse> recharge(
            @Valid @RequestBody RechargeAlimtalkCreditRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    RECHARGE_SCOPE,
                    request,
                    () -> ResponseEntity.ok(alimtalkCreditCommandService.recharge(tenantId, request))
            );
        }
        return ResponseEntity.ok(alimtalkCreditCommandService.recharge(tenantId, request));
    }

    @PutMapping("/auto-recharge")
    public ResponseEntity<AlimtalkCreditResponse> configureAutoRecharge(
            @Valid @RequestBody ConfigureAutoRechargeRequest request) {
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(alimtalkCreditCommandService.enableAutoRecharge(tenantId, request));
    }

    @DeleteMapping("/auto-recharge")
    public ResponseEntity<AlimtalkCreditResponse> disableAutoRecharge() {
        Long tenantId = TenantContextHolder.getTenantId();
        return ResponseEntity.ok(alimtalkCreditCommandService.disableAutoRecharge(tenantId));
    }
}
