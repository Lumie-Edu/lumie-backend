package com.lumie.common.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts the {@code sort} property names accepted by a {@link org.springframework.data.domain.Pageable}-bound
 * controller method to a known allowlist.
 *
 * <p>Without this guard, any string passed as {@code ?sort=...} is forwarded to JPA as a
 * property reference. That leaks internal column names, allows expensive sorts on
 * non-indexed columns, and risks {@code PropertyReferenceException} 500s for typos.
 *
 * <p>Enforced by {@link SortFieldAllowlistInterceptor}, which inspects the request's
 * {@code sort} parameters before the controller method runs and rejects unknown
 * properties with {@code 400 Bad Request}.
 *
 * <p>Example:
 * <pre>{@code
 * @GetMapping
 * @AllowedSortFields({"createdAt", "name", "email"})
 * public ResponseEntity<Page<StudentResponse>> getStudents(
 *         @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedSortFields {
    String[] value();
}
