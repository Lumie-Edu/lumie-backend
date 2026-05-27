package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Alimtalk credit balance per tenant.
 * tenant_id is UNIQUE — one credit account per tenant.
 *
 * <p>Auto-recharge fires when balance drops below {@code autoRechargeThreshold}
 * (checked by {@link #shouldAutoRecharge()}).
 */
@Entity
@Table(name = "alimtalk_credits", indexes = {
        @Index(name = "idx_alimtalk_credits_tenant_id", columnList = "tenant_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlimtalkCredit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Column(name = "balance", nullable = false)
    private int balance;

    @Column(name = "auto_recharge_enabled", nullable = false)
    private boolean autoRechargeEnabled;

    @Column(name = "auto_recharge_threshold")
    private Integer autoRechargeThreshold;

    @Column(name = "auto_recharge_package")
    private Integer autoRechargePackage;

    @Builder
    private AlimtalkCredit(Long tenantId, int initialBalance) {
        this.tenantId = tenantId;
        this.balance = initialBalance;
        this.autoRechargeEnabled = false;
    }

    public static AlimtalkCredit create(Long tenantId) {
        return AlimtalkCredit.builder()
                .tenantId(tenantId)
                .initialBalance(0)
                .build();
    }

    // ─── Domain methods ───────────────────────────────────────────────────────

    /**
     * Deducts {@code n} credits from the balance.
     * Throws {@link BillingException} if balance is insufficient.
     */
    public void consume(int n) {
        if (n <= 0) {
            throw new BillingException(BillingErrorCode.INTERNAL_ERROR,
                    "Consumption amount must be positive");
        }
        if (this.balance < n) {
            throw new BillingException(BillingErrorCode.ALIMTALK_CREDIT_INSUFFICIENT,
                    "Insufficient alimtalk credit: balance=%d, requested=%d".formatted(this.balance, n));
        }
        this.balance -= n;
    }

    /** Adds {@code packageSize} credits to the balance (post-payment). */
    public void recharge(int packageSize) {
        if (packageSize <= 0) {
            throw new BillingException(BillingErrorCode.INTERNAL_ERROR,
                    "Recharge package size must be positive");
        }
        this.balance += packageSize;
    }

    public void enableAutoRecharge(int threshold, int packageSize) {
        if (threshold <= 0 || packageSize <= 0) {
            throw new BillingException(BillingErrorCode.INTERNAL_ERROR,
                    "Auto-recharge threshold and package must be positive");
        }
        this.autoRechargeEnabled = true;
        this.autoRechargeThreshold = threshold;
        this.autoRechargePackage = packageSize;
    }

    public void disableAutoRecharge() {
        this.autoRechargeEnabled = false;
        this.autoRechargeThreshold = null;
        this.autoRechargePackage = null;
    }

    // ─── Query methods ────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when auto-recharge is enabled and the current balance
     * is at or below the configured threshold.
     */
    public boolean shouldAutoRecharge() {
        return this.autoRechargeEnabled
                && this.autoRechargeThreshold != null
                && this.balance <= this.autoRechargeThreshold;
    }

    // ─── equals / hashCode (ID-based) ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlimtalkCredit other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
