package com.lumie.billing.domain.exception;

public class InvalidSubscriptionStateException extends RuntimeException {

    public InvalidSubscriptionStateException(String message) {
        super(message);
    }
}
