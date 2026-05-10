package com.lumie.common.idempotency;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;

/**
 * Raised when an {@code Idempotency-Key} is reused with a request body whose
 * canonical fingerprint differs from the original — Stripe's "key collision"
 * semantics. Maps to HTTP 409 Conflict via {@link CommonErrorCode#DUPLICATE_RESOURCE}.
 */
public class IdempotencyConflictException extends BusinessException {

    public IdempotencyConflictException(String idempotencyKey) {
        super(CommonErrorCode.DUPLICATE_RESOURCE,
              "Idempotency-Key '%s' was reused with a different request body".formatted(idempotencyKey));
    }
}
