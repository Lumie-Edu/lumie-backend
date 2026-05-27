package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.RegisterBillingKeyRequest;
import com.lumie.billing.application.dto.response.BillingKeyResponse;
import com.lumie.billing.application.port.out.BillingKeyPersistencePort;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.entity.Subscription;
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
     * Issues a Toss billing key from a one-time auth code and registers it for the tenant.
     * If the tenant has an existing active subscription without a billing key, it is attached.
     */
    public BillingKeyResponse registerBillingKey(Long tenantId, RegisterBillingKeyRequest request,
                                                  String idempotencyKey) {
        if (billingKeyPersistencePort.existsActiveByTenantId(tenantId)) {
            throw new BillingException(BillingErrorCode.BILLING_KEY_ALREADY_ACTIVE,
                    "Tenant already has an active billing key: " + tenantId);
        }

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
        BillingKey billingKey = BillingKey.register(tenantId, result.billingKey(),
                request.customerKey(), result.maskedCardNumber(), result.cardCompany());

        BillingKey saved = billingKeyPersistenceHelper.save(billingKey);
        log.info("BillingKey registered: id={}, tenantId={}", saved.getId(), tenantId);

        // Attach to active subscription if one exists without a billing key
        subscriptionPersistencePort.findByTenantId(tenantId).ifPresent(sub -> {
            if (!sub.hasBillingKey()) {
                sub.attachBillingKey(saved.getId());
                subscriptionPersistenceHelper.save(sub);
                log.info("BillingKey attached to subscription: subscriptionId={}, billingKeyId={}",
                        sub.getId(), saved.getId());
            }
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
