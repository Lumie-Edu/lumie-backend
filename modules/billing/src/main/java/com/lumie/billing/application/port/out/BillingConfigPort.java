package com.lumie.billing.application.port.out;

/**
 * Outbound port for the subset of billing configuration that application
 * services need. Implementations live in {@code adapter/out/config/}
 * (typically backed by Spring {@code @ConfigurationProperties}).
 *
 * <p>Exists so {@code application/service/*} classes do not import
 * {@code adapter/out/config/BillingProperties} directly — that would invert
 * the hexagonal dependency direction (application must not depend on
 * adapter). Adapter-internal users (e.g. {@code TossPaymentClient}) may
 * still bind to {@code BillingProperties} directly because they live on the
 * same side of the boundary.
 *
 * <p>Only fields meaningful to application orchestration are exposed —
 * raw Toss secret key / webhook secret / tax-invoice credentials stay
 * inside {@code BillingProperties} so they cannot leak into application
 * code that has no business handling them.
 */
public interface BillingConfigPort {

    /** Plan id used when provisioning a FREE subscription. Defaults to {@code "FREE"}. */
    String getFreePlanId();

    /** Toss publishable client key — safe to expose to browsers. */
    String getTossClientKey();

    /** Whether Toss is in sandbox mode. */
    boolean isTossSandbox();
}
