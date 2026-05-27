package com.lumie.tuition.adapter.in.internal;

import com.lumie.tuition.application.service.TuitionInvoiceQueryService;
import com.lumie.tuition.application.service.TuitionPaymentQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Placeholder implementation for the tuition inter-module API.
 *
 * <p>Other modules (e.g. the academy dashboard stats) that need to query tuition
 * data should declare a {@code TuitionService} interface in {@code libs/internal-api}
 * under the {@code com.lumie.tuition.api} package and implement it here.
 *
 * <p>TODO (Task 5): define {@code TuitionService} in libs/internal-api, then
 * add {@code implements TuitionService} here and wire the required queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TuitionServiceAdapter {

    private final TuitionInvoiceQueryService invoiceQueryService;
    private final TuitionPaymentQueryService paymentQueryService;

    // Placeholder: expose aggregate stats for the academy owner dashboard.
    // Example future method:
    //   public TuitionSummary getSummary(String tenantSlug) { ... }
}
