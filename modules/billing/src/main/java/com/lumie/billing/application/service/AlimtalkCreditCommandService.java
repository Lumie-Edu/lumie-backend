package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.ConfigureAutoRechargeRequest;
import com.lumie.billing.application.dto.request.RechargeAlimtalkCreditRequest;
import com.lumie.billing.application.dto.response.AlimtalkCreditResponse;
import com.lumie.billing.application.port.out.AlimtalkCreditPersistencePort;
import com.lumie.billing.domain.entity.AlimtalkCredit;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages alimtalk credit recharge, consumption, and auto-recharge settings.
 *
 * <p>NOTE: actual billing for recharge (PG call) is done by the caller before invoking
 * {@link #recharge} — this service only updates the balance post-payment confirmation.
 * Consumption is called from the notification module via libs/internal-api (BillingService.consumeAlimtalkCredit).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlimtalkCreditCommandService {

    private final AlimtalkCreditPersistencePort alimtalkCreditPersistencePort;
    private final AlimtalkCreditPersistenceHelper alimtalkCreditPersistenceHelper;

    @Transactional
    public AlimtalkCreditResponse recharge(Long tenantId, RechargeAlimtalkCreditRequest request) {
        AlimtalkCredit credit = getOrCreateCreditAccount(tenantId);
        credit.recharge(request.packageSize());
        AlimtalkCredit saved = alimtalkCreditPersistencePort.save(credit);
        log.info("AlimtalkCredit recharged: tenantId={}, packageSize={}, newBalance={}",
                tenantId, request.packageSize(), saved.getBalance());
        return AlimtalkCreditResponse.from(saved);
    }

    @Transactional
    public AlimtalkCreditResponse enableAutoRecharge(Long tenantId, ConfigureAutoRechargeRequest request) {
        AlimtalkCredit credit = getOrCreateCreditAccount(tenantId);
        credit.enableAutoRecharge(request.threshold(), request.packageSize());
        AlimtalkCredit saved = alimtalkCreditPersistencePort.save(credit);
        log.info("AlimtalkCredit auto-recharge enabled: tenantId={}, threshold={}, package={}",
                tenantId, request.threshold(), request.packageSize());
        return AlimtalkCreditResponse.from(saved);
    }

    @Transactional
    public AlimtalkCreditResponse disableAutoRecharge(Long tenantId) {
        AlimtalkCredit credit = findCreditOrThrow(tenantId);
        credit.disableAutoRecharge();
        AlimtalkCredit saved = alimtalkCreditPersistencePort.save(credit);
        log.info("AlimtalkCredit auto-recharge disabled: tenantId={}", tenantId);
        return AlimtalkCreditResponse.from(saved);
    }

    /**
     * Consumes {@code count} credits.
     * Called from libs/internal-api by the notification module.
     * Uses helper (REQUIRES_NEW-capable) to allow consumption in a sub-transaction if needed.
     */
    public AlimtalkCreditResponse consume(Long tenantId, int count) {
        AlimtalkCredit saved = alimtalkCreditPersistenceHelper.consume(tenantId, count);
        log.debug("AlimtalkCredit consumed: tenantId={}, count={}, newBalance={}", tenantId, count, saved.getBalance());
        return AlimtalkCreditResponse.from(saved);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private AlimtalkCredit getOrCreateCreditAccount(Long tenantId) {
        return alimtalkCreditPersistencePort.findByTenantId(tenantId)
                .orElseGet(() -> {
                    AlimtalkCredit newCredit = AlimtalkCredit.create(tenantId);
                    return alimtalkCreditPersistencePort.save(newCredit);
                });
    }

    private AlimtalkCredit findCreditOrThrow(Long tenantId) {
        return alimtalkCreditPersistencePort.findByTenantId(tenantId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.ALIMTALK_CREDIT_NOT_FOUND,
                        "AlimtalkCredit account not found for tenant: " + tenantId));
    }
}
