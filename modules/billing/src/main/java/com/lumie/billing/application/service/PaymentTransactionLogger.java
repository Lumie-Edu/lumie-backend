package com.lumie.billing.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.application.port.out.PaymentTransactionPersistencePort;
import com.lumie.billing.domain.entity.PaymentTransaction;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Persists external billing call audit entries in a SEPARATE transaction (REQUIRES_NEW).
 * This ensures audit records survive even if the outer transaction rolls back.
 *
 * <p>Usage: call {@code log(...)} both before (request) and after (response) each provider call.
 * The caller is responsible for constructing a redacted payload map.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionLogger {

    private final PaymentTransactionPersistencePort transactionPersistencePort;
    private final ObjectMapper objectMapper;

    /**
     * Persists one audit record. Uses REQUIRES_NEW so it commits independently.
     *
     * @param tenantId        nullable for platform-level transactions
     * @param type            type of external call
     * @param direction       REQUEST (before call), RESPONSE (after call), or WEBHOOK (inbound)
     * @param idempotencyKey  nullable
     * @param ownerEntity     entity class name, e.g. "Invoice"
     * @param ownerId         entity ID, nullable
     * @param payloadMap      redacted request/response details
     * @param httpStatus      HTTP status when available (null for SDK calls)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTransaction log(Long tenantId, TransactionType type,
                                   TransactionDirection direction,
                                   String idempotencyKey,
                                   String ownerEntity, Long ownerId,
                                   Map<String, Object> payloadMap,
                                   Integer httpStatus) {
        validateNewAuditOwner(type, ownerEntity);
        JsonNode pgPayload = objectMapper.valueToTree(payloadMap);
        PaymentTransaction tx = PaymentTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .type(type)
                .direction(direction)
                .idempotencyKey(idempotencyKey)
                .ownerEntity(ownerEntity)
                .ownerId(ownerId)
                .pgPayload(pgPayload)
                .httpStatus(httpStatus)
                .build();

        PaymentTransaction saved = transactionPersistencePort.save(tx);
        log.debug("PaymentTransaction logged: id={}, type={}, owner={}/{}, httpStatus={}",
                saved.getId(), type, ownerEntity, ownerId, httpStatus);
        return saved;
    }

    private static void validateNewAuditOwner(TransactionType type, String ownerEntity) {
        boolean supported = switch (type) {
            case SUBSCRIPTION_CHARGE, SUBSCRIPTION_REFUND ->
                    "Invoice".equals(ownerEntity) || "Subscription".equals(ownerEntity);
            case ALIMTALK_RECHARGE -> "AlimtalkCredit".equals(ownerEntity);
            case BILLING_KEY_ISSUE, BILLING_KEY_REVOKE -> "BillingKey".equals(ownerEntity);
            case TAX_INVOICE_ISSUE -> "TaxInvoice".equals(ownerEntity);
        };
        if (!supported) {
            throw new IllegalArgumentException("Unsupported billing audit owner for " + type + ": " + ownerEntity);
        }
    }
}
