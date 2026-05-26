package com.lumie.common.exception;

import java.util.Map;

/**
 * Implemented by {@link BusinessException} subclasses that carry context fields
 * beyond {@link ErrorCode} + {@code message}.
 *
 * <p>The returned map is copied into the RFC 7807 {@code ProblemDetail.properties}
 * by {@code GlobalExceptionHandler}, so subclasses do not need to depend on
 * Spring HTTP types — keeping {@code domain/exception/} packages Spring-free
 * per backend rules.
 *
 * <p>Keys correspond to the JSON field names the FE consumes (typically
 * camelCase). Values are serialized by Jackson; prefer plain
 * {@code String}/{@code Number}/{@code Enum}/{@code Boolean} over custom types.
 */
public interface ExtensionProperties {

    /**
     * @return ordered map of extension fields to publish on the error response;
     *         {@link java.util.LinkedHashMap} recommended to keep client-visible
     *         field ordering deterministic.
     */
    Map<String, Object> extensionProperties();
}
