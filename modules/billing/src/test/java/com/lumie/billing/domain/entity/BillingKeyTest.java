package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.BillingKeyStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingKeyTest {

    @Test
    void register_creates_ACTIVE_key() {
        BillingKey key = BillingKey.register(1L, "billing-key-abc", "lumie-tenant-1",
                "4242-****-****-4242", "VISA");

        assertThat(key.getStatus()).isEqualTo(BillingKeyStatus.ACTIVE);
        assertThat(key.isActive()).isTrue();
        assertThat(key.getRevokedAt()).isNull();
        assertThat(key.getCustomerKey()).isEqualTo("lumie-tenant-1");
    }

    @Test
    void revoke_transitions_to_REVOKED() {
        BillingKey key = BillingKey.register(1L, "billing-key-abc", "lumie-tenant-1",
                "4242-****-****-4242", "VISA");
        key.revoke();

        assertThat(key.getStatus()).isEqualTo(BillingKeyStatus.REVOKED);
        assertThat(key.isActive()).isFalse();
        assertThat(key.getRevokedAt()).isNotNull();
    }

    @Test
    void revoke_already_revoked_throws() {
        BillingKey key = BillingKey.register(1L, "billing-key-abc", "lumie-tenant-1",
                "4242-****-****-4242", "VISA");
        key.revoke();

        assertThatThrownBy(key::revoke).isInstanceOf(BillingException.class);
    }
}
