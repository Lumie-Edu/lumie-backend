package com.lumie.billing.adapter.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Immutable configuration properties for the billing module.
 * Bound via constructor (record-style) — no @Setter needed.
 *
 * <p>Toss credentials come from Vault via VaultStaticSecret (common-values.yaml).
 */
@ConfigurationProperties(prefix = "billing")
public record BillingProperties(Toss toss, TaxInvoice taxInvoice, Subscription subscription) {

    public BillingProperties {
        if (toss == null) {
            toss = new Toss(true, null, null, null);
        }
        if (taxInvoice == null) {
            taxInvoice = new TaxInvoice("000-00-00000", null, null);
        }
        if (subscription == null) {
            subscription = new Subscription("FREE");
        }
    }

    public record Toss(boolean sandbox, String secretKey, String clientKey, String webhookSecret) {}

    public record TaxInvoice(String supplierBusinessNo, String popbillLinkId, String popbillSecretKey) {}

    /**
     * Subscription-level settings.
     * {@code freePlanId} is the {@code plans.id} value used when provisioning a new tenant.
     * Defaults to {@code "FREE"} to match the seed data in the billing module migrations.
     */
    public record Subscription(String freePlanId) {}
}
