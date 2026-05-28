package com.lumie.billing.adapter.in.event;

import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.tenant.api.TenantCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Adapter-layer test: verifies the listener delegates the event payload to
 * {@link SubscriptionCommandService#provisionTrial}. Business behavior
 * (idempotency, PRO plan lookup, factory call) is covered by
 * {@code SubscriptionCommandServiceTest#provisionTrial_*}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantCreatedListener — delegates to SubscriptionCommandService")
class TenantCreatedListenerTest {

    private static final Long TENANT_ID = 42L;
    private static final String TENANT_SLUG = "demo-academy";

    @Mock private SubscriptionCommandService subscriptionCommandService;

    @InjectMocks
    private TenantCreatedListener listener;

    @Test
    @DisplayName("onTenantCreated_delegatesToProvisionTrial")
    void onTenantCreated_delegatesToProvisionTrial() {
        TenantCreatedEvent event = new TenantCreatedEvent(TENANT_ID, TENANT_SLUG);

        listener.onTenantCreated(event);

        verify(subscriptionCommandService).provisionTrial(TENANT_ID, TENANT_SLUG);
        verifyNoMoreInteractions(subscriptionCommandService);
    }
}
