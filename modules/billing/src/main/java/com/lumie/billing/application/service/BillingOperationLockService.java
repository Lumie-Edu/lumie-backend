package com.lumie.billing.application.service;

import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * DB-level mutex for in-flight billing operations.
 *
 * <p>Serializes concurrent billing flows (e.g. two browser tabs both hitting
 * {@code POST /v1/billing/subscribe} for the same tenant with different
 * Idempotency-Keys). Uses {@code INSERT ... ON CONFLICT DO NOTHING} against
 * the {@code (tenant_id, operation_type)} primary key so the DB itself is
 * the arbiter — no application-level lock contention.
 *
 * <p>The lock row is always deleted in a {@code finally} block (separate
 * {@code REQUIRES_NEW} transaction) so crashes do not permanently block
 * the tenant. A cleanup scheduler removes rows older than 15 minutes as
 * a defense-in-depth backstop.
 *
 * <p>Usage:
 * <pre>
 *   return operationLockService.executeExclusively(
 *           tenantId, "SUBSCRIBE", idempotencyKey,
 *           () -> doActualSubscribeWork(...));
 * </pre>
 */
@Slf4j
@Service
public class BillingOperationLockService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate requiresNewTx;

    public BillingOperationLockService(JdbcTemplate jdbcTemplate,
                                       PlatformTransactionManager txManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.requiresNewTx = new TransactionTemplate(txManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Acquire an exclusive per-tenant, per-operation lock, execute {@code action},
     * then release the lock.
     *
     * @param tenantId       tenant performing the operation.
     * @param operationType  logical operation name — {@code SUBSCRIBE}, {@code CHANGE_PLAN}, etc.
     * @param idempotencyKey caller's Idempotency-Key, stored for debugging.
     * @param action         the work to execute under the lock.
     * @param <T>            return type of the action.
     * @return the value returned by {@code action}.
     * @throws BillingException with {@link BillingErrorCode#OPERATION_IN_PROGRESS} (HTTP 409)
     *                          when another in-flight operation already holds the lock.
     */
    public <T> T executeExclusively(Long tenantId, String operationType,
                                    String idempotencyKey, Supplier<T> action) {
        acquireLock(tenantId, operationType, idempotencyKey);
        try {
            return action.get();
        } finally {
            releaseLock(tenantId, operationType);
        }
    }

    private void acquireLock(Long tenantId, String operationType, String idempotencyKey) {
        Integer inserted = requiresNewTx.execute(status ->
                jdbcTemplate.update(
                        """
                        INSERT INTO public.billing_operation_locks
                            (tenant_id, operation_type, idempotency_key)
                        VALUES (?, ?, ?)
                        ON CONFLICT DO NOTHING
                        """,
                        tenantId, operationType, idempotencyKey
                )
        );
        if (inserted == null || inserted == 0) {
            log.warn("BillingOperationLock: tenant={} op={} already in progress (key={})",
                    tenantId, operationType, idempotencyKey);
            throw new BillingException(BillingErrorCode.OPERATION_IN_PROGRESS,
                    "Another billing operation is already in flight for this tenant");
        }
        log.debug("BillingOperationLock: acquired tenant={} op={}", tenantId, operationType);
    }

    private void releaseLock(Long tenantId, String operationType) {
        requiresNewTx.executeWithoutResult(status ->
                jdbcTemplate.update(
                        "DELETE FROM public.billing_operation_locks " +
                        "WHERE tenant_id = ? AND operation_type = ?",
                        tenantId, operationType
                )
        );
        log.debug("BillingOperationLock: released tenant={} op={}", tenantId, operationType);
    }
}
