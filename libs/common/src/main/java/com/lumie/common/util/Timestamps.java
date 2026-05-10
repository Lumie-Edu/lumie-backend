package com.lumie.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Boundary helper for converting JPA-stored {@link LocalDateTime} values into
 * the timezone-aware {@link Instant} that DTOs expose to API consumers.
 *
 * <p>Backend audit timestamps live in {@code BaseEntity} as {@code LocalDateTime}
 * because Hibernate's PostgreSQL mapping uses {@code timestamp without time
 * zone} columns. The JVM is locked to UTC (see Dockerfile {@code ENV TZ=UTC}
 * and {@code -Duser.timezone=UTC}) so {@code LocalDateTime.now()} writes UTC
 * wall-clock — and converting back via {@link ZoneOffset#UTC} round-trips
 * losslessly.
 *
 * <p>Use this helper at every entity → response-DTO boundary. Direct
 * conversion calls in factories invite drift if a future contributor forgets
 * the offset; centralizing keeps the assumption auditable.
 */
public final class Timestamps {

    private Timestamps() {}

    /**
     * Convert a UTC-wall-clock {@link LocalDateTime} into an {@link Instant}.
     * Returns {@code null} when input is null so DTO factories can pass through
     * nullable audit timestamps without a guard at every call site.
     */
    public static Instant utcInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Convert an API-boundary {@link Instant} back into a UTC-wall-clock
     * {@link LocalDateTime} for storage by JPA entities whose columns are
     * still {@code timestamp without time zone}. Mirror of {@link #utcInstant}.
     */
    public static LocalDateTime utcLocal(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
