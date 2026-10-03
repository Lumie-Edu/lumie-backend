package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.TaxInvoicePersistencePort;
import com.lumie.billing.application.port.out.TaxInvoicePort;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.TaxInvoice;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.Money;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxInvoiceCommandServiceTest {

    @Mock private InvoicePersistencePort invoicePersistencePort;
    @Mock private TaxInvoicePersistencePort taxInvoicePersistencePort;
    @Mock private TaxInvoicePersistenceHelper taxInvoicePersistenceHelper;
    @Mock private TaxInvoicePort taxInvoicePort;
    @Mock private PaymentTransactionLogger transactionLogger;
    @Mock private BillingOperationLockService operationLockService;
    @InjectMocks private TaxInvoiceCommandService service;

    private TaxInvoice saved;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "supplierBusinessNo", "000-00-00000");
        Invoice invoice = Invoice.create("I-1", 2L, 3L, Money.of(1000), Money.of(100),
                "Subscription", Instant.now(), Instant.now(), Instant.now(), "order-1");
        ReflectionTestUtils.setField(invoice, "id", 1L);
        invoice.markPaid("payment-1", Instant.now());
        when(invoicePersistencePort.findById(1L)).thenReturn(Optional.of(invoice));
        when(operationLockService.executeExclusively(eq(3L), eq("TAX_INVOICE_ISSUE_1"),
                eq("tax-invoice-1"), any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(3)).get());
        saved = TaxInvoice.create(1L, 3L, "123-45-67890", "000-00-00000",
                Money.of(1000), Money.of(100));
        ReflectionTestUtils.setField(saved, "id", 4L);
        when(taxInvoicePersistenceHelper.save(any(TaxInvoice.class))).thenReturn(saved);
    }

    @Test
    void recordsProviderRequestAndSuccessAsTaxInvoiceAudit() {
        when(taxInvoicePort.issue(any())).thenReturn(new TaxInvoicePort.TaxInvoiceResult(true, "external-1", null));
        when(taxInvoicePersistencePort.findById(4L)).thenReturn(Optional.of(saved));

        service.issueForInvoice(1L, "123-45-67890");

        InOrder order = inOrder(transactionLogger, taxInvoicePort);
        order.verify(transactionLogger).log(eq(3L), eq(TransactionType.TAX_INVOICE_ISSUE),
                eq(TransactionDirection.REQUEST), any(), eq("TaxInvoice"), eq(4L), any(), eq(null));
        order.verify(taxInvoicePort).issue(any());
        order.verify(transactionLogger).log(eq(3L), eq(TransactionType.TAX_INVOICE_ISSUE),
                eq(TransactionDirection.RESPONSE), any(), eq("TaxInvoice"), eq(4L), any(), eq(null));
        verify(taxInvoicePersistenceHelper).markIssued(eq(4L), eq("external-1"), any());
        ArgumentCaptor<TaxInvoicePort.TaxInvoiceRequest> request =
                ArgumentCaptor.forClass(TaxInvoicePort.TaxInvoiceRequest.class);
        verify(taxInvoicePort).issue(request.capture());
        assertThat(request.getValue().idempotencyKey()).isEqualTo("tax-invoice-1");
    }

    @Test
    void recordsProviderExceptionAndMarksTaxInvoiceFailed() {
        when(taxInvoicePort.issue(any())).thenThrow(new IllegalStateException("provider unavailable"));

        assertThatThrownBy(() -> service.issueForInvoice(1L, "123-45-67890"))
                .isInstanceOf(BillingException.class);

        verify(transactionLogger).log(eq(3L), eq(TransactionType.TAX_INVOICE_ISSUE),
                eq(TransactionDirection.RESPONSE), any(), eq("TaxInvoice"), eq(4L), any(), eq(null));
        verify(taxInvoicePersistenceHelper).markFailed(4L, "IllegalStateException");
    }
}
