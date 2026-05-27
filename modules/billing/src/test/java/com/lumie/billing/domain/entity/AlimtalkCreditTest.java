package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlimtalkCreditTest {

    @Test
    void create_initializes_with_zero_balance() {
        AlimtalkCredit credit = AlimtalkCredit.create(1L);

        assertThat(credit.getBalance()).isZero();
        assertThat(credit.isAutoRechargeEnabled()).isFalse();
    }

    @Test
    void recharge_increases_balance() {
        AlimtalkCredit credit = AlimtalkCredit.create(1L);
        credit.recharge(500);

        assertThat(credit.getBalance()).isEqualTo(500);
    }

    @Test
    void consume_decreases_balance() {
        AlimtalkCredit credit = AlimtalkCredit.create(1L);
        credit.recharge(100);
        credit.consume(30);

        assertThat(credit.getBalance()).isEqualTo(70);
    }

    @Test
    void consume_more_than_balance_throws() {
        AlimtalkCredit credit = AlimtalkCredit.create(1L);
        credit.recharge(10);

        assertThatThrownBy(() -> credit.consume(11))
                .isInstanceOf(BillingException.class);
    }

    @Test
    void shouldAutoRecharge_true_when_below_threshold() {
        AlimtalkCredit credit = AlimtalkCredit.create(1L);
        credit.recharge(100);
        credit.enableAutoRecharge(50, 500);
        credit.consume(60);

        assertThat(credit.shouldAutoRecharge()).isTrue();
    }

    @Test
    void shouldAutoRecharge_false_when_above_threshold() {
        AlimtalkCredit credit = AlimtalkCredit.create(1L);
        credit.recharge(100);
        credit.enableAutoRecharge(50, 500);

        assertThat(credit.shouldAutoRecharge()).isFalse();
    }

    @Test
    void disableAutoRecharge_clears_settings() {
        AlimtalkCredit credit = AlimtalkCredit.create(1L);
        credit.enableAutoRecharge(50, 500);
        credit.disableAutoRecharge();

        assertThat(credit.isAutoRechargeEnabled()).isFalse();
        assertThat(credit.getAutoRechargeThreshold()).isNull();
        assertThat(credit.getAutoRechargePackage()).isNull();
    }
}
