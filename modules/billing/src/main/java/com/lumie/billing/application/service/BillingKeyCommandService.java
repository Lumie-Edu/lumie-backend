package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.RegisterBillingKeyRequest;
import com.lumie.billing.application.dto.response.BillingKeyResponse;
import com.lumie.billing.application.port.out.BillingKeyPersistencePort;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Manages billing key lifecycle for a tenant.
 * No class-level @Transactional — PG calls must stay outside transaction (HARD_RULES).
 * DB writes delegated to {@link BillingKeyPersistenceHelper}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingKeyCommandService {

    private final BillingKeyPersistencePort billingKeyPersistencePort;
    private final BillingKeyPersistenceHelper billingKeyPersistenceHelper;
    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final SubscriptionPersistenceHelper subscriptionPersistenceHelper;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentTransactionLogger transactionLogger;

    /**
     * Issues a Toss billing key from a one-time auth code and makes it the
     * tenant's active payment method.
     *
     * <p>Idempotent upsert semantics: if the tenant already has an active key,
     * it is revoked atomically with the new INSERT and the subscription is
     * re-pointed at the new key. This is the single path for first-time
     * registration, card swap, and user retry — the operation truthfully
     * means "this card is now the tenant's active billing method", not "create
     * a never-before-seen key" (which earlier returned 409 and stranded the
     * user whenever a previous attempt left state behind).
     */
    public BillingKeyResponse registerBillingKey(Long tenantId, RegisterBillingKeyRequest request,
                                                  String idempotencyKey) {
        // Log request (outside @Transactional — audit must persist even on PG failure)
        transactionLogger.log(tenantId, TransactionType.BILLING_KEY_ISSUE,
                TransactionDirection.REQUEST, idempotencyKey,
                "BillingKey", null,
                Map.of("authKey", "[REDACTED]", "customerKey", request.customerKey()),
                null);

        // PG call — outside @Transactional boundary
        PaymentGatewayPort.BillingKeyIssueResult result =
                paymentGatewayPort.issueBillingKey(request.authKey(), request.customerKey());

        transactionLogger.log(tenantId, TransactionType.BILLING_KEY_ISSUE,
                TransactionDirection.RESPONSE, idempotencyKey,
                "BillingKey", null,
                Map.of("success", result.success(),
                        "maskedCardNumber", result.maskedCardNumber() != null ? result.maskedCardNumber() : "",
                        "cardCompany", result.cardCompany() != null ? result.cardCompany() : "",
                        "error", result.errorMessage() != null ? result.errorMessage() : ""),
                result.success() ? 200 : 400);

        if (!result.success()) {
            throw new BillingException(BillingErrorCode.TOSS_API_ERROR,
                    "Failed to issue billing key: " + result.errorMessage());
        }

        // customerKey is the value passed to Toss at issuance time (from request.customerKey()).
        // We store it so chargeWithBillingKey can pass the exact same value back to Toss.
        BillingKey newBillingKey = BillingKey.register(tenantId, result.billingKey(),
                request.customerKey(), result.maskedCardNumber(), result.cardCompany());

        // Atomically: REVOKE existing active (if any) + INSERT new active.
        // Partial unique index (uq_billing_keys_tenant_active) allows this in
        // a single transaction.
        BillingKey saved = billingKeyPersistenceHelper.replaceActive(newBillingKey);
        log.info("BillingKey registered: id={}, tenantId={}", saved.getId(), tenantId);

        // Re-point any tenant subscription at the new key (unconditional —
        // we just replaced the active key, so the old reference is stale).
        subscriptionPersistencePort.findByTenantId(tenantId).ifPresent(sub -> {
            sub.attachBillingKey(saved.getId());
            subscriptionPersistenceHelper.save(sub);
            log.info("BillingKey attached to subscription: subscriptionId={}, billingKeyId={}",
                    sub.getId(), saved.getId());
        });

        return BillingKeyResponse.from(saved);
    }

    /**
     * Revokes the tenant's active billing key and pauses any active subscription.
     */
    public void revokeBillingKey(Long tenantId, Long billingKeyId) {
        BillingKey billingKey = billingKeyPersistencePort.findById(billingKeyId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.BILLING_KEY_NOT_FOUND,
                        "BillingKey not found: " + billingKeyId));

        if (!billingKey.getTenantId().equals(tenantId)) {
            throw new BillingException(BillingErrorCode.BILLING_KEY_NOT_FOUND,
                    "BillingKey does not belong to tenant: " + tenantId);
        }

        transactionLogger.log(tenantId, TransactionType.BILLING_KEY_REVOKE,
                TransactionDirection.REQUEST, null, "BillingKey", billingKeyId,
                Map.of("tenantId", tenantId), null);

        billingKeyPersistenceHelper.revoke(billingKeyId);

        transactionLogger.log(tenantId, TransactionType.BILLING_KEY_REVOKE,
                TransactionDirection.RESPONSE, null, "BillingKey", billingKeyId,
                Map.of("tenantId", tenantId, "success", true), 200);

        log.info("BillingKey revoked: id={}, tenantId={}", billingKeyId, tenantId);

        // Pause active subscription
        subscriptionPersistencePort.findByTenantId(tenantId).ifPresent(sub -> {
            if (sub.isActive() || com.lumie.billing.domain.vo.SubscriptionStatus.PAST_DUE.equals(sub.getStatus())) {
                sub.pause(com.lumie.billing.domain.vo.SuspensionReason.BILLING_KEY_REVOKED);
                subscriptionPersistenceHelper.save(sub);
                log.info("Subscription paused due to billing key revocation: subscriptionId={}", sub.getId());
            }
        });
    }
}
