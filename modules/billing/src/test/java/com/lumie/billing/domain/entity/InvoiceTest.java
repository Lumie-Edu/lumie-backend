package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.InvoiceStatus;
import com.lumie.billing.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTest {

    @Test
    void create_initializes_as_PENDING() {
        Invoice invoice = createInvoice();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void markPaid_transitions_to_PAID() {
        Invoice invoice = createInvoice();
        Instant paidAt = Instant.now();

        invoice.markPaid("payment-key-123", paidAt);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getPaymentKey()).isEqualTo("payment-key-123");
        assertThat(invoice.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void markFailed_transitions_to_FAILED() {
        Invoice invoice = createInvoice();
        invoice.markFailed("Insufficient funds");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.FAILED);
        assertThat(invoice.getFailureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void markRefunded_after_PAID_transitions_to_REFUNDED() {
        Invoice invoice = createInvoice();
        invoice.markPaid("pk", Instant.now());
        invoice.markRefunded();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REFUNDED);
    }

    @Test
    void markPaid_when_not_PENDING_throws() {
        Invoice invoice = createInvoice();
        invoice.markPaid("pk", Instant.now());

        assertThatThrownBy(() -> invoice.markPaid("pk2", Instant.now()))
                .isInstanceOf(BillingException.class);
    }

    @Test
    void isOverdue_returns_true_for_past_due_PENDING() {
        Invoice invoice = Invoice.create(
                "INV-001", 1L, 1L,
                Money.of(50_000L), Money.zero(),
                "Test",
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1),
                "ORDER-001"
        );

        assertThat(invoice.isOverdue()).isTrue();
    }

    private Invoice createInvoice() {
        return Invoice.create(
                "INV-TEST-001", 1L, 1L,
                Money.of(100_000L), Money.of(9_090L),
                "Test subscription",
                Instant.now(),
                Instant.now().plusSeconds(2592000),
                Instant.now().plusSeconds(2592000),
                "ORDER-TEST-001"
        );
    }
}
