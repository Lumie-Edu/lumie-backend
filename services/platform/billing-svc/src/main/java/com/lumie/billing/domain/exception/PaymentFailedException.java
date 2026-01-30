package com.lumie.billing.domain.exception;

import lombok.Getter;

@Getter
public class PaymentFailedException extends RuntimeException {

    private final String paymentKey;
    private final String errorCode;

    public PaymentFailedException(String message) {
        super(message);
        this.paymentKey = null;
        this.errorCode = null;
    }

    public PaymentFailedException(String message, String paymentKey, String errorCode) {
        super(message);
        this.paymentKey = paymentKey;
        this.errorCode = errorCode;
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
        this.paymentKey = null;
        this.errorCode = null;
    }
}
