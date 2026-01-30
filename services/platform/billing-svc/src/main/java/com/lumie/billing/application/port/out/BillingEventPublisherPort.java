package com.lumie.billing.application.port.out;

import com.lumie.messaging.event.BillingEvent;

public interface BillingEventPublisherPort {
    void publish(BillingEvent event);
}
