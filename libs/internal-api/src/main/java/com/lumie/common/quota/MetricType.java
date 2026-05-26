package com.lumie.common.quota;

/**
 * Cross-cutting quota metric identifier.
 * <p>
 * Used by {@code QuotaExceededException} and the billing/quota internal API to
 * expose the constrained dimension in a typed (rather than free-form String) shape,
 * so that downstream consumers (REST clients, ProblemDetail consumers, UI upsell
 * flows) can branch on it without parsing messages.
 * <p>
 * Previously housed under {@code billing.domain.vo} — promoted to
 * {@code libs/common} because quota concepts span billing, staff, and student
 * modules.
 */
public enum MetricType {
    STUDENTS,
    ACADEMIES,
    ADMINS,
    OMR_MONTHLY
}
