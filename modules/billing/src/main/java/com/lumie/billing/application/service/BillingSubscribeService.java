package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.SubscribeRequest;
import com.lumie.billing.application.dto.response.BillingKeyResponse;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.dto.response.SubscribeResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Atomic "subscribe to a paid plan with a new card" orchestrator.
 *
 * <p>This is the single business operation behind
 * {@code POST /v1/billing/subscribe}, replacing the FE-coordinated
 * {@code registerBillingKey + changePlan + chargeFirstInvoice} chain that
 * left tenants stranded mid-flow whenever any step failed.
 *
 * <p>No class-level {@code @Transactional}: the PG calls must live outside
 * any JDBC tx (HARD_RULES). DB mutations are committed through short
 * helper-scoped transactions; each step is individually safe to retry:
 *
 * <ol>
 *   <li>(PG, tx-free) issue billing key at Toss</li>
 *   <li>(helper tx) {@code replaceActive}: REVOKE existing active + INSERT new</li>
 *   <li>(helper tx) {@code attachBillingKeyAndSubscribe}: subscription→new
 *       key + new plan in one DB tx</li>
 *   <li>(PG via {@link SubscriptionChargeExecutor}) charge the first invoice;
 *       executor commits the Invoice + advances {@code nextChargeAt}
 *       in its own short tx</li>
 * </ol>
 *
 * <p>Failure modes:
 * <ul>
 *   <li>Step 1 fails → no DB state mutated; user can retry from a fresh
 *       Toss widget run.</li>
 *   <li>Step 2 or 3 fails → DB rolls back step 2 (helper-tx-scoped). User
 *       retries the whole call; {@code replaceActive} is idempotent.</li>
 *   <li>Step 4 fails (PG charge declines) → tenant is on the new plan with
 *       the new card, no successful invoice. Caller surface is
 *       {@code BillingException(PAYMENT_FAILED)} → 402. The monthly charge
 *       scheduler picks the subscription up at the next cycle and retries
 *       (existing 3-strikes → PAUSE policy).</li>
 * </ul>
 *
 * <p>{@code idempotencyKey} is enforced at the controller via
 * {@link com.lumie.common.idempotency.IdempotencyService} — duplicate
 * requests within the dedup window return the original {@link SubscribeResponse}
 * without re-executing this orchestration.
 *
 * <p>Concurrent calls from the same tenant with different Idempotency-Keys are
 * serialized by {@link BillingOperationLockService} — the second concurrent
 * call receives HTTP 409 {@code OPERATION_IN_PROGRESS} rather than
 * re-issuing a Toss billing key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingSubscribeService {

    private final PaymentGatewayPort paymentGatewayPort;
    private final PlanPersistencePort planPersistencePort;
    private final BillingKeyPersistenceHelper billingKeyPersistenceHelper;
    private final SubscriptionPersistenceHelper subscriptionPersistenceHelper;
    private final SubscriptionChargeExecutor chargeExecutor;
    private final PaymentTransactionLogger transactionLogger;
    private final BillingOperationLockService operationLockService;

    public SubscribeResponse subscribe(Long tenantId, String tenantSlug,
                                       SubscribeRequest request, String idempotencyKey) {
        return operationLockService.executeExclusively(tenantId, "SUBSCRIBE", idempotencyKey, () -> {
            log.info("BillingSubscribe.subscribe: tenant={}, planId={}", tenantSlug, request.planId());

            Plan newPlan = planPersistencePort.findById(request.planId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan", request.planId()));
            if (newPlan.isFree()) {
                throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                        "Cannot subscribe to a free plan via /billing/subscribe: " + request.planId());
            }

            // 1. PG issueBillingKey (outside any tx)
            transactionLogger.log(tenantId, TransactionType.BILLING_KEY_ISSUE,
                    TransactionDirection.REQUEST, idempotencyKey, "BillingKey", null,
                    Map.of("authKey", "[REDACTED]", "customerKey", request.customerKey()),
                    null);

            PaymentGatewayPort.BillingKeyIssueResult issueResult =
                    paymentGatewayPort.issueBillingKey(request.authKey(), request.customerKey());

            transactionLogger.log(tenantId, TransactionType.BILLING_KEY_ISSUE,
                    TransactionDirection.RESPONSE, idempotencyKey, "BillingKey", null,
                    Map.of("success", issueResult.success(),
                            "error", issueResult.errorMessage() != null ? issueResult.errorMessage() : ""),
                    issueResult.success() ? 200 : 400);

            if (!issueResult.success()) {
                throw new BillingException(BillingErrorCode.TOSS_API_ERROR,
                        "Failed to issue billing key: " + issueResult.errorMessage());
            }

            // 2. replaceActive: REVOKE old + INSERT new (single helper tx)
            BillingKey newBillingKey = BillingKey.register(tenantId,
                    issueResult.billingKey(), request.customerKey(),
                    issueResult.maskedCardNumber(), issueResult.cardCompany());
            BillingKey savedKey = billingKeyPersistenceHelper.replaceActive(newBillingKey);
            log.info("BillingSubscribe: billing key persisted id={}", savedKey.getId());

            // 3. Attach + subscribe (single helper tx). Subscription
            //    domain method absorbs the state-machine adapter (ACTIVE / PAUSED /
            //    PAST_DUE / CANCELLED / EXPIRED → ACTIVE on newPlan), so this
            //    endpoint is the single entry point for any "subscribe to paid"
            //    intent regardless of the tenant's prior subscription state.
            //
            //    nextChargeAt = now + 1 month matches the convention used in
            //    SubscriptionChargeExecutor.createInvoiceAndAdvanceChargeCycle so
            //    the value is stable across the subsequent charge (executor will
            //    overwrite via chargeSucceeded(periodEnd) on success, but the
            //    initial stamp is required by Subscription.subscribe contract).
            java.time.Instant initialNextChargeAt = java.time.Instant.now()
                    .atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant();
            Subscription subscription = subscriptionPersistenceHelper
                    .attachBillingKeyAndSubscribe(tenantSlug, savedKey.getId(), newPlan,
                            initialNextChargeAt);
            log.info("BillingSubscribe: subscription upgraded id={} → planId={}",
                    subscription.getId(), newPlan.getId());

            // 4. Charge (PG outside tx; executor commits Invoice + nextChargeAt in short tx)
            SubscriptionChargeExecutor.ChargeOutcome outcome = chargeExecutor.charge(subscription);
            return switch (outcome.status()) {
                case PAID -> new SubscribeResponse(
                        SubscriptionResponse.from(subscription),
                        BillingKeyResponse.from(savedKey),
                        InvoiceResponse.from(outcome.invoice()));
                case FAILED -> throw new BillingException(BillingErrorCode.PAYMENT_FAILED,
                        outcome.reason() != null ? outcome.reason() : "PG charge failed");
                case SKIPPED -> throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                        "Charge skipped: " + (outcome.reason() != null ? outcome.reason() : "unknown"));
            };
        });
    }
}
