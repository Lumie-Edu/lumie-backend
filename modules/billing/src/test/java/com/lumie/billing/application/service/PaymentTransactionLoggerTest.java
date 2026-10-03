package com.lumie.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.application.port.out.PaymentTransactionPersistencePort;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PaymentTransactionLoggerTest {

    @Test
    void rejectsUnsupportedNewOwnerAndTypePairBeforePersisting() {
        PaymentTransactionPersistencePort port = mock(PaymentTransactionPersistencePort.class);
        PaymentTransactionLogger logger = new PaymentTransactionLogger(port, new ObjectMapper());

        assertThatThrownBy(() -> logger.log(1L, TransactionType.TAX_INVOICE_ISSUE,
                TransactionDirection.REQUEST, "request-1", "Invoice", 7L, Map.of("invoiceId", 7L), null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(port);
    }
}
