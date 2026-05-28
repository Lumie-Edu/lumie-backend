package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.BillingKeyPersistencePort;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.vo.BillingKeyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingKeyPersistenceHelper.replaceActive")
class BillingKeyPersistenceHelperTest {

    private static final Long TENANT_ID = 15L;

    @Mock private BillingKeyPersistencePort port;
    @InjectMocks private BillingKeyPersistenceHelper helper;

    @Test
    @DisplayName("existing active key가 있으면 REVOKE를 saveAndFlush로 (지연 flush 방지) 후 새 키 INSERT")
    void replaceActive_revokesExistingViaSaveAndFlush_thenSavesNew() {
        BillingKey existing = BillingKey.register(TENANT_ID, "pg_old", "lumie-tenant-15",
                "1111-****-****-1111", "현대");
        ReflectionTestUtils.setField(existing, "id", 1L);

        BillingKey incoming = BillingKey.register(TENANT_ID, "pg_new", "lumie-tenant-15",
                "2222-****-****-2222", "신한");

        given(port.findActiveByTenantId(TENANT_ID)).willReturn(Optional.of(existing));
        given(port.saveAndFlush(existing)).willReturn(existing);
        given(port.save(incoming)).willReturn(incoming);

        BillingKey result = helper.replaceActive(incoming);

        assertThat(result).isSameAs(incoming);
        assertThat(existing.getStatus()).isEqualTo(BillingKeyStatus.REVOKED);

        // Order-of-writes is the bug we're guarding against: saveAndFlush of
        // the REVOKE must complete before the INSERT of the new ACTIVE row,
        // otherwise uq_billing_keys_tenant_active fires 23505.
        InOrder order = inOrder(port);
        order.verify(port).findActiveByTenantId(TENANT_ID);
        order.verify(port).saveAndFlush(existing);
        order.verify(port).save(incoming);

        // Plain save() on the existing key would defer the UPDATE to commit
        // time, inverting order. Lock that contract.
        verify(port, never()).save(existing);
    }

    @Test
    @DisplayName("existing active key가 없으면 그냥 INSERT (saveAndFlush 호출 없음)")
    void replaceActive_noExisting_justSavesNew() {
        BillingKey incoming = BillingKey.register(TENANT_ID, "pg_new", "lumie-tenant-15",
                "2222-****-****-2222", "신한");

        given(port.findActiveByTenantId(TENANT_ID)).willReturn(Optional.empty());
        given(port.save(incoming)).willReturn(incoming);

        BillingKey result = helper.replaceActive(incoming);

        assertThat(result).isSameAs(incoming);
        verify(port, never()).saveAndFlush(any());
        verify(port).save(incoming);
    }
}
