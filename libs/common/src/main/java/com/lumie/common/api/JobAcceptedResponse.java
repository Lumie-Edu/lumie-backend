package com.lumie.common.api;

/**
 * Standard response body for asynchronous (long-job) POST endpoints.
 *
 * <p>Paired with {@code 202 Accepted} and a {@code Location} header pointing to
 * the same {@code statusUrl}. Clients may either poll {@code statusUrl} directly
 * or read the {@code Location} header — both carry the same path.
 *
 * <p>{@code jobId} is exposed as {@code Long} to preserve wire compatibility with
 * existing TanStack Query hooks that type it as {@code number}.
 */
public record JobAcceptedResponse(Long jobId, String statusUrl) {
}
