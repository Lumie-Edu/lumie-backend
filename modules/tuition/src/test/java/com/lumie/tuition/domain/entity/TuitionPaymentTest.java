package com.lumie.tuition.domain.entity;

import com.lumie.tuition.domain.vo.Money;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.vo.TuitionPaymentMethod;
import com.lumie.tuition.domain.vo.TuitionPaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TuitionPaymentTest {

    @Test
    void capture_transitions_to_CAPTURED() {
        TuitionPayment payment = createPendingPayment();
        payment.capture("pg-tx-001", Instant.now());
        assertThat(payment.getStatus()).isEqualTo(TuitionPaymentStatus.CAPTURED);
        assertThat(payment.getPgTransactionId()).isEqualTo("pg-tx-001");
        assertThat(payment.getCapturedAt()).isNotNull();
    }

    @Test
    void fail_transitions_to_FAILED() {
        TuitionPayment payment = createPendingPayment();
        payment.fail("Card declined");
        assertThat(payment.getStatus()).isEqualTo(TuitionPaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Card declined");
    }

    @Test
    void refund_partial_stays_CAPTURED() {
        TuitionPayment payment = createPendingPayment();
        payment.capture("pg-tx-002", Instant.now());
        payment.refund(Money.of(100_000L), "Partial refund", 99L);
        assertThat(payment.getStatus()).isEqualTo(TuitionPaymentStatus.CAPTURED);
        assertThat(payment.getRefundedAmount().getAmount().longValue()).isEqualTo(100_000L);
    }

    @Test
    void refund_full_transitions_to_CANCELLED() {
        TuitionPayment payment = createPendingPayment();
        payment.capture("pg-tx-003", Instant.now());
        payment.refund(Money.of(300_000L), "Full refund", 99L);
        assertThat(payment.getStatus()).isEqualTo(TuitionPaymentStatus.CANCELLED);
    }

    @Test
    void refund_exceeds_amount_throws() {
        TuitionPayment payment = createPendingPayment();
        payment.capture("pg-tx-004", Instant.now());
        assertThatThrownBy(() -> payment.refund(Money.of(999_999L), "Over-refund", 99L))
                .isInstanceOf(TuitionException.class);
    }

    @Test
    void capture_already_captured_throws() {
        TuitionPayment payment = createPendingPayment();
        payment.capture("pg-tx-005", Instant.now());
        assertThatThrownBy(() -> payment.capture("pg-tx-006", Instant.now()))
                .isInstanceOf(TuitionException.class);
    }

    private TuitionPayment createPendingPayment() {
        return TuitionPayment.create(1L, "TUITION-1-abc123456789", Money.of(300_000L), TuitionPaymentMethod.CARD);
    }
}
