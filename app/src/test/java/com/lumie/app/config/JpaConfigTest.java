package com.lumie.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JpaConfigTest {

    @Test
    void requiresNewTransactionTemplate_usesRequiresNewPropagation() {
        JpaConfig config = new JpaConfig();
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

        var template = config.requiresNewTransactionTemplate(txManager);

        assertThat(template.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void defaultTransactionTemplate_keepsRequiredPropagation() {
        JpaConfig config = new JpaConfig();
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

        var template = config.transactionTemplate(txManager);

        assertThat(template.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    }
}
