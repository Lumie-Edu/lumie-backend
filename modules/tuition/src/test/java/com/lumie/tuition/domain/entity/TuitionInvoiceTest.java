package com.lumie.tuition.domain.entity;

import com.lumie.tuition.domain.vo.Money;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.vo.LineItem;
import com.lumie.tuition.domain.vo.TuitionInvoiceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TuitionInvoiceTest {

    @Test
    void issue_transitions_to_ISSUED() {
        TuitionInvoice invoice = createDraftInvoice();
        invoice.issue(Instant.now().plusSeconds(86400));
        assertThat(invoice.getStatus()).isEqualTo(TuitionInvoiceStatus.ISSUED);
        assertThat(invoice.getIssuedAt()).isNotNull();
        assertThat(invoice.getDueDate()).isNotNull();
    }

    @Test
    void issue_from_non_DRAFT_throws() {
        TuitionInvoice invoice = createDraftInvoice();
        invoice.issue(Instant.now().plusSeconds(86400));
        assertThatThrownBy(() -> invoice.issue(Instant.now().plusSeconds(86400)))
                .isInstanceOf(TuitionException.class);
    }

    @Test
    void markPaid_transitions_to_PAID() {
        TuitionInvoice invoice = createDraftInvoice();
        invoice.issue(Instant.now().plusSeconds(86400));
        invoice.markPaid(Instant.now());
        assertThat(invoice.getStatus()).isEqualTo(TuitionInvoiceStatus.PAID);
        assertThat(invoice.getPaidAt()).isNotNull();
    }

    @Test
    void cancel_from_ISSUED_transitions_to_CANCELLED() {
        TuitionInvoice invoice = createDraftInvoice();
        invoice.issue(Instant.now().plusSeconds(86400));
        invoice.cancel("Changed mind");
        assertThat(invoice.getStatus()).isEqualTo(TuitionInvoiceStatus.CANCELLED);
        assertThat(invoice.getCancellationReason()).isEqualTo("Changed mind");
    }

    @Test
    void cancel_PAID_invoice_throws() {
        TuitionInvoice invoice = createDraftInvoice();
        invoice.issue(Instant.now().plusSeconds(86400));
        invoice.markPaid(Instant.now());
        assertThatThrownBy(() -> invoice.cancel("oops"))
                .isInstanceOf(TuitionException.class);
    }

    @Test
    void markOverdue_transitions_ISSUED_to_OVERDUE() {
        TuitionInvoice invoice = createDraftInvoice();
        invoice.issue(Instant.now().minusSeconds(1)); // dueDate in the past
        invoice.markOverdue();
        assertThat(invoice.getStatus()).isEqualTo(TuitionInvoiceStatus.OVERDUE);
    }

    @Test
    void isOverdueByDate_returns_true_for_past_due_date() {
        TuitionInvoice invoice = createDraftInvoice();
        invoice.issue(Instant.now().minusSeconds(3600));
        assertThat(invoice.isOverdueByDate()).isTrue();
    }

    private TuitionInvoice createDraftInvoice() {
        List<LineItem> items = List.of(
                LineItem.of("수학 수강료", 1, Money.of(300_000L))
        );
        return TuitionInvoice.create(
                1L, null, null,
                "2월 수강료",
                items,
                Money.of(300_000L),
                Money.of(27_272L),
                "idem-test-001"
        );
    }
}
