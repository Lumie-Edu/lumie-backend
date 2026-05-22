package com.lumie.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Cursor (keyset) pagination response.
 *
 * <p>{@code nextCursor} is an opaque token the client passes back as
 * {@code ?cursor=} to fetch the next slice. {@code null} indicates there is
 * no further page (the returned {@code items} are the last slice).
 *
 * <p>No total count: cursor pagination intentionally avoids the COUNT(*)
 * cost; callers that need a count should use a dedicated stats endpoint.
 */
public record CursorResponse<T>(List<T> items, @Schema(nullable = true) String nextCursor, int limit) {

    public CursorResponse {
        Objects.requireNonNull(items, "items must not be null");
    }

    public static <T> CursorResponse<T> of(List<T> items, String nextCursor, int limit) {
        return new CursorResponse<>(items, nextCursor, limit);
    }
}
