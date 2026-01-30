package com.lumie.messaging.config;

public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    // Exchanges
    public static final String LUMIE_EVENTS_EXCHANGE = "lumie.events";
    public static final String LUMIE_DLX_EXCHANGE = "lumie.dlx";

    // Queues - Tenant
    public static final String TENANT_SCHEMA_PROVISIONING_QUEUE = "tenant.schema-provisioning";
    public static final String TENANT_SCHEMA_PROVISIONING_DLQ = "tenant.schema-provisioning.dlq";

    // Queues - Billing
    public static final String BILLING_SUBSCRIPTION_QUEUE = "billing.subscription";
    public static final String BILLING_SUBSCRIPTION_DLQ = "billing.subscription.dlq";
    public static final String BILLING_QUOTA_QUEUE = "billing.quota";
    public static final String BILLING_QUOTA_DLQ = "billing.quota.dlq";

    // Routing Keys - Tenant
    public static final String TENANT_CREATED_ROUTING_KEY = "tenant.created.*";
    public static final String TENANT_READY_ROUTING_KEY = "tenant.ready.*";

    // Routing Keys - Billing
    public static final String BILLING_SUBSCRIPTION_CREATED_ROUTING_KEY = "billing.subscription.created.*";
    public static final String BILLING_SUBSCRIPTION_UPGRADED_ROUTING_KEY = "billing.subscription.upgraded.*";
    public static final String BILLING_QUOTA_EXCEEDED_ROUTING_KEY = "billing.quota.exceeded.*";
}
