package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.BillingConfigResponse;
import com.lumie.billing.application.port.out.BillingConfigPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Read-only query service that exposes the browser-safe subset of Toss
 * configuration (publishable client key + sandbox flag).
 *
 * <p>Depends on {@link BillingConfigPort} (application-layer interface)
 * rather than the adapter-side {@code BillingProperties} record so the
 * application layer never reaches across the hexagonal boundary. The port
 * intentionally exposes only the fields safe to surface — secret key and
 * webhook secret cannot leak into the response even by mistake.
 */
@Service
@RequiredArgsConstructor
public class BillingConfigQueryService {

    private final BillingConfigPort billingConfig;

    public BillingConfigResponse getPublicConfig() {
        return new BillingConfigResponse(
                billingConfig.getTossClientKey(),
                billingConfig.isTossSandbox()
        );
    }
}
