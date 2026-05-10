package com.lumie.common.idempotency;

import java.time.Instant;

/**
 * Row representation of {@code public.idempotency_keys}. Plain record (not a
 * JPA entity) — this table is cross-tenant and accessed via {@code JdbcTemplate}
 * to bypass Hibernate's schema-per-tenant routing.
 */
public record IdempotencyKeyRecord(
        Long id,
        String tenantSlug,
        String endpointPath,
        String idempotencyKey,
        String requestFingerprint,
        int responseStatus,
        String responseBody,
        Instant createdAt
) {
}
