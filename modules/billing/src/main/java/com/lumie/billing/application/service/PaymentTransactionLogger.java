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
 * Persists PG audit log entries in a SEPARATE transaction (REQUIRES_NEW).
 * This ensures audit records survive even if the outer transaction rolls back.
 *
 * <p>Usage: call {@code log(...)} both before (request) and after (response) each PG call.
 * The caller (TossPaymentClient or service) is responsible for constructing the payload map.
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
     * @param type            type of PG call
     * @param direction       REQUEST (before PG call), RESPONSE (after PG call), or WEBHOOK (inbound)
     * @param idempotencyKey  nullable
     * @param ownerEntity     entity class name, e.g. "Invoice"
     * @param ownerId         entity ID, nullable
     * @param payloadMap      raw PG request/response payload
     * @param httpStatus      HTTP status returned by PG (or null for request-side log)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTransaction log(Long tenantId, TransactionType type,
                                   TransactionDirection direction,
                                   String idempotencyKey,
                                   String ownerEntity, Long ownerId,
                                   Map<String, Object> payloadMap,
                                   Integer httpStatus) {
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
}
