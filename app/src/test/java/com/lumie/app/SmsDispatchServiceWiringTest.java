package com.lumie.app;

import com.lumie.common.tx.RequiresNewTransaction;
import com.lumie.notification.application.service.SmsDispatchService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SmsDispatchServiceWiringTest extends FullContextTestBase {

    @Autowired
    private SmsDispatchService smsDispatchService;

    @Autowired
    @RequiresNewTransaction
    private TransactionTemplate requiresNewTransactionTemplate;

    @Test
    void smsDispatchServiceReceivesRequiresNewTransactionTemplate() {
        Object actual = ReflectionTestUtils.getField(
                smsDispatchService,
                "dispatchTransactionTemplate"
        );

        assertThat(actual).isSameAs(requiresNewTransactionTemplate);
        assertThat(((TransactionTemplate) actual).getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
}
