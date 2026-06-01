package com.lumie.tuition.application.dto.request;

import com.lumie.tuition.domain.vo.TuitionPaymentMethod;
import jakarta.validation.constraints.NotNull;

/**
 * Initiates a tuition collection for an invoice. Under the PaySsam model the gateway
 * sends the invoice to the guardian's phone (resolved from the invoice), so no
 * checkout success/fail redirect URLs are needed.
 */
public record RequestPaymentRequest(
        @NotNull Long invoiceId,
        @NotNull TuitionPaymentMethod method
) {}
