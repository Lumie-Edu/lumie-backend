package com.lumie.tuition.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Korean Won (KRW) monetary value — integer scale, no decimal cents.
 * Mirrors {@code com.lumie.billing.domain.vo.Money} but lives in the tuition
 * domain to avoid a cross-module domain layer import (HARD_RULES).
 *
 * <p>Column mapping is intentionally left to the embedding entity via
 * {@code @AttributeOverride} — no {@code @Column} annotation here.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Money {

    private BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(0, RoundingMode.HALF_UP);
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public long toLong() {
        return this.amount.longValue();
    }

    @Override
    public String toString() {
        return String.format("%,d원", amount.longValue());
    }
}
