package com.lumie.billing.domain.exception;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.ExtensionProperties;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown when payment processing or its precondition fails.
 * <p>
 * Tier-1 typed business exception: {@link #paymentKey} and {@link #providerErrorCode}
 * survive into {@code ProblemDetail.properties} so the client can correlate to
 * the upstream Toss/Paddle invoice and present an actionable retry CTA.
 */
@Getter
public class PaymentFailedException extends BusinessException implements ExtensionProperties {

    private final String paymentKey;
    private final String providerErrorCode;

    public PaymentFailedException(String message) {
        super(BillingErrorCode.PAYMENT_FAILED, message);
        this.paymentKey = null;
        this.providerErrorCode = null;
    }

    public PaymentFailedException(String message, String paymentKey, String providerErrorCode) {
        super(BillingErrorCode.PAYMENT_FAILED, message);
        this.paymentKey = paymentKey;
        this.providerErrorCode = providerErrorCode;
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(BillingErrorCode.PAYMENT_FAILED, message, cause);
        this.paymentKey = null;
        this.providerErrorCode = null;
    }

    @Override
    public Map<String, Object> extensionProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (paymentKey != null) properties.put("paymentKey", paymentKey);
        if (providerErrorCode != null) properties.put("providerErrorCode", providerErrorCode);
        return properties;
    }
}
