package com.lumie.billing.application.service;

import com.lumie.billing.adapter.out.config.BillingProperties;
import com.lumie.billing.application.dto.response.BillingConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Read-only query service that exposes the browser-safe subset of Toss
 * configuration (publishable client key + sandbox flag).
 *
 * <p>Existence rationale: keeps the controller from importing
 * {@code adapter/out/config/BillingProperties} directly — that's a
 * cross-adapter dependency the hexagonal rule forbids. The application
 * layer owns the "which config fields are safe to expose" decision so the
 * secret + webhook fields can never accidentally land in the response.
 */
@Service
@RequiredArgsConstructor
public class BillingConfigQueryService {

    private final BillingProperties billingProperties;

    public BillingConfigResponse getPublicConfig() {
        return new BillingConfigResponse(
                billingProperties.toss().clientKey(),
                billingProperties.toss().sandbox()
        );
    }
}
