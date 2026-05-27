package com.lumie.tuition.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tuition.application.dto.request.TossWebhookPayload;
import com.lumie.tuition.domain.entity.TuitionInvoice;
import com.lumie.tuition.domain.repository.TuitionInvoiceRepository;
import com.lumie.tuition.domain.repository.TuitionPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Application service that handles business logic for Toss payment webhook events.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Resolve tenant from orderId (payment → invoice → tenantId)</li>
 *   <li>Set TenantContextHolder so RLS filters apply during processing</li>
 *   <li>Delegate state transitions to TuitionPaymentCommandService</li>
 * </ul>
 *
 * <p>Tenant slug resolution is a temporary lookup via TuitionInvoice.tenantId.
 * TODO (Task 5): inject TenantService (libs/internal-api) and replace resolveTenantSlug.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TuitionWebhookApplicationService {

    private final TuitionPaymentRepository paymentRepository;
    private final TuitionInvoiceRepository invoiceRepository;
    private final TuitionPaymentCommandService paymentCommandService;

    public void handlePaymentWebhook(TossWebhookPayload payload) {
        if (payload.data() == null) {
            log.warn("Toss webhook payload has no data — ignoring");
            return;
        }

        String orderId = payload.data().orderId();

        // Resolve tenant from orderId — find payment first, then invoice
        var paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.warn("Toss webhook: no payment found for orderId={}. Ignoring.", orderId);
            return;
        }

        Long invoiceId = paymentOpt.get().getTuitionInvoiceId();
        TuitionInvoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            log.error("Toss webhook: invoice {} not found for orderId={}", invoiceId, orderId);
            return;
        }

        try {
            Long tenantId = invoice.getTenantId();
            String tenantSlug = resolveTenantSlug(tenantId);
            TenantContextHolder.withinContext(tenantSlug, tenantId, (Runnable) () -> {
                if (payload.isPaymentDone()) {
                    Instant capturedAt = payload.data().approvedAt() != null
                            ? payload.data().approvedAt()
                            : Instant.now();
                    paymentCommandService.confirmPaymentFromWebhook(
                            orderId,
                            payload.data().paymentKey(),
                            payload.data().totalAmount(),
                            capturedAt
                    );
                }
            });
        } catch (Exception e) {
            log.error("Error processing Toss tuition webhook for orderId={}: {}", orderId, e.getMessage(), e);
            // Always swallow here — caller returns 200 to prevent Toss from retrying indefinitely.
            // The error is logged; manual reconciliation handles edge cases.
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Resolves a tenant slug from the RLS tenant_id.
     * This is a temporary lookup — the tenant module exposes TenantService via internal-api.
     * TODO (Task 5): inject TenantService (libs/internal-api) and call tenantService.getTenantById(tenantId).
     */
    private String resolveTenantSlug(Long tenantId) {
        // Placeholder: TenantService lookup will be wired in Task 5 integration phase.
        throw new UnsupportedOperationException(
                "resolveTenantSlug: inject TenantService from libs/internal-api and call getTenantById(" + tenantId + ")");
    }
}
