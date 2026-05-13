package com.lumie.common.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint as deprecated per RFC 8594.
 * The {@link SunsetInterceptor} adds a {@code Sunset:} response header with the
 * given ISO-8601 date so API consumers can schedule migration.
 *
 * <pre>
 * {@code @Sunset("2026-12-01")}
 * {@code @GetMapping("/v1/old-path")}
 * public ResponseEntity<...> deprecated() { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sunset {
    /** ISO-8601 date (e.g. {@code "2026-12-01"}) after which the endpoint will be removed. */
    String value();
}
