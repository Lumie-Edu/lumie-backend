package com.lumie.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Stripe-style idempotency-key gate for replay-safe POST endpoints.
 *
 * <p>Usage from a controller:
 * <pre>
 *   if (idempotencyKey != null) {
 *       return idempotencyService.executeOnce(
 *               idempotencyKey,
 *               "POST /v1/payments/confirm",
 *               request,
 *               () -> ResponseEntity.ok(paymentService.confirmPayment(request))
 *       );
 *   }
 * </pre>
 *
 * <p>Semantics:
 * <ul>
 *   <li>Same key + same fingerprint → return cached response.</li>
 *   <li>Same key + different fingerprint → throw {@link IdempotencyConflictException} (HTTP 409).</li>
 *   <li>First call → run the action, persist {@code (status, body)} keyed by {@code (tenant, path, key)}.</li>
 * </ul>
 *
 * <p>The persistence write runs in a {@code REQUIRES_NEW} transaction so the
 * idempotency row commits independently of any tx the wrapped action started.
 */
@Slf4j
@Service
public class IdempotencyService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper canonicalMapper;
    private final TransactionTemplate requiresNewTx;

    public IdempotencyService(JdbcTemplate jdbcTemplate, PlatformTransactionManager txManager) {
        this.jdbcTemplate = jdbcTemplate;
        // Independent, deterministic mapper for fingerprint hashing + response
        // body storage. Sorted Map entries AND alphabetical POJO/record fields
        // keep the fingerprint stable regardless of map iteration or reflection
        // order; JavaTimeModule with ISO-8601 output (not numeric timestamps)
        // handles Instant/Java-8 time DTO fields. Deliberately NOT the API
        // ObjectMapper bean — fingerprints must not drift if the API's Jackson
        // config changes, which would turn in-flight idempotency keys into
        // spurious 409 conflicts.
        this.canonicalMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        this.requiresNewTx = new TransactionTemplate(txManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private static final RowMapper<IdempotencyKeyRecord> ROW_MAPPER = (rs, rowNum) -> new IdempotencyKeyRecord(
            rs.getLong("id"),
            rs.getString("tenant_slug"),
            rs.getString("endpoint_path"),
            rs.getString("idempotency_key"),
            rs.getString("request_fingerprint"),
            rs.getInt("response_status"),
            rs.getString("response_body"),
            rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant()
    );

    /**
     * Run {@code action} at most once per (tenant, endpointPath, key) tuple.
     *
     * @param idempotencyKey caller-supplied key (any string up to 255 chars).
     * @param endpointPath   logical scope (e.g. {@code "POST /v1/payments/confirm"}).
     * @param requestBody    request payload — fingerprinted for collision detection.
     * @param action         the work to perform on a first call. Must return
     *                       {@code ResponseEntity<T>} so the status + body can be
     *                       cached for replay.
     */
    public <T> ResponseEntity<T> executeOnce(
            String idempotencyKey,
            String endpointPath,
            Object requestBody,
            Supplier<ResponseEntity<T>> action
    ) {
        return executeOnce(idempotencyKey, endpointPath, requestBody, true, action);
    }

    /**
     * Run {@code action} once while storing only replay metadata, not the
     * response body. Use this for endpoints whose successful response may carry
     * phone numbers, rendered message bodies, or other recipient PII.
     */
    public <T> ResponseEntity<T> executeOnceWithoutResponseBody(
            String idempotencyKey,
            String endpointPath,
            Object requestBody,
            Supplier<ResponseEntity<T>> action
    ) {
        return executeOnce(idempotencyKey, endpointPath, requestBody, false, action);
    }

    private <T> ResponseEntity<T> executeOnce(
            String idempotencyKey,
            String endpointPath,
            Object requestBody,
            boolean storeResponseBody,
            Supplier<ResponseEntity<T>> action
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // Caller chose not to enable idempotency (header missing or empty).
            return action.get();
        }
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String fingerprint = fingerprint(requestBody);

        Optional<IdempotencyKeyRecord> existing = findExisting(tenantSlug, endpointPath, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), idempotencyKey, fingerprint);
        }

        ResponseEntity<T> response = action.get();
        try {
            persist(tenantSlug, endpointPath, idempotencyKey, fingerprint, response, storeResponseBody);
        } catch (DuplicateKeyException raceLost) {
            // A concurrent first call beat us. Re-read and replay whatever's there.
            log.debug("Idempotency-Key write lost race for key={}, replaying", idempotencyKey);
            IdempotencyKeyRecord winner = findExisting(tenantSlug, endpointPath, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency row vanished after duplicate-key on key=" + idempotencyKey));
            return replay(winner, idempotencyKey, fingerprint);
        }
        return response;
    }

    private Optional<IdempotencyKeyRecord> findExisting(String tenantSlug, String endpointPath, String key) {
        try {
            IdempotencyKeyRecord row = jdbcTemplate.queryForObject(
                    """
                    SELECT id, tenant_slug, endpoint_path, idempotency_key,
                           request_fingerprint, response_status, response_body, created_at
                      FROM public.idempotency_keys
                     WHERE tenant_slug = ?
                       AND endpoint_path = ?
                       AND idempotency_key = ?
                    """,
                    ROW_MAPPER,
                    tenantSlug, endpointPath, key
            );
            return Optional.ofNullable(row);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> replay(IdempotencyKeyRecord cached, String key, String fingerprint) {
        if (!cached.requestFingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(key);
        }
        Object body = null;
        if (cached.responseBody() != null) {
            try {
                body = canonicalMapper.readValue(cached.responseBody(), Object.class);
            } catch (JsonProcessingException e) {
                // Body wasn't JSON — return raw string.
                body = cached.responseBody();
            }
        }
        return (ResponseEntity<T>) ResponseEntity.status(cached.responseStatus()).body(body);
    }

    private void persist(
            String tenantSlug,
            String endpointPath,
            String idempotencyKey,
            String fingerprint,
            ResponseEntity<?> response,
            boolean storeResponseBody
    ) {
        String body = storeResponseBody ? serialize(response.getBody()) : null;
        requiresNewTx.executeWithoutResult(status ->
                jdbcTemplate.update(
                        """
                        INSERT INTO public.idempotency_keys
                            (tenant_slug, endpoint_path, idempotency_key,
                             request_fingerprint, response_status, response_body)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        tenantSlug, endpointPath, idempotencyKey,
                        fingerprint, response.getStatusCode().value(), body
                )
        );
    }

    private String serialize(Object body) {
        if (body == null) return null;
        try {
            return canonicalMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            // The missing-JavaTimeModule root cause is fixed, so a failure here
            // now means a genuinely non-serializable body. Fail fast instead of
            // storing a toString() that would corrupt the fingerprint and the
            // replayed response (HARD_RULES: no workaround when the root cause
            // is known).
            throw new IllegalStateException("Failed to serialize idempotency body", e);
        }
    }

    /**
     * SHA-256 of the canonical JSON serialization of {@code body}. Sorted-keys
     * canonicalization makes the fingerprint stable across map iteration order.
     */
    String fingerprint(Object body) {
        String canonical = body == null ? "null" : serialize(body);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java spec — unreachable.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
