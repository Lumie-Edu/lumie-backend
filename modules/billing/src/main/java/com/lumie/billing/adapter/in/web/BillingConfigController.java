package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.response.BillingConfigResponse;
import com.lumie.billing.application.service.BillingConfigQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Exposes browser-safe billing configuration (Toss client key + sandbox flag)
 * so the FE no longer needs a build-time NEXT_PUBLIC_* env or per-developer
 * .env.local entry — single source of truth is the Vault-injected BE env.
 *
 * <p>Plumbing: Vault → VSO → K8s Secret → BE pod env → BillingProperties →
 * {@link BillingConfigQueryService} → this endpoint → FE TanStack Query.
 *
 * <p>Cache-Control: 1h private — the value only changes on a BE deploy, and
 * the FE's TanStack {@code staleTime} only governs the in-memory cache; this
 * header lets the browser HTTP cache absorb hard refreshes too.
 */
@RestController
@RequestMapping("/v1/billing/config")
@RequiredArgsConstructor
public class BillingConfigController {

    private final BillingConfigQueryService billingConfigQueryService;

    @GetMapping
    public ResponseEntity<BillingConfigResponse> getBillingConfig() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(billingConfigQueryService.getPublicConfig());
    }
}
