package com.lumie.billing.application.port.out;

public interface PaddleGatewayPort {

    record PaddleSubscription(String subscriptionId, String customerId, String status,
                               String priceId, String nextBillingAt) {}

    record PaddleCustomer(String customerId, String email, String name) {}

    PaddleSubscription getSubscription(String paddleSubscriptionId);

    PaddleSubscription cancelSubscription(String paddleSubscriptionId, boolean effectAtPeriodEnd);

    PaddleSubscription updateSubscription(String paddleSubscriptionId, String newPriceId);

    PaddleCustomer createCustomer(String email, String name);
}
