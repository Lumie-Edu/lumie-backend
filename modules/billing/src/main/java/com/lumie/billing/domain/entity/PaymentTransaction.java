package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

/**
 * Append-only audit log for every PG call (request + response).
 *
 * <p>Intentionally immutable after creation:
 * <ul>
 *   <li>No @Version — never updated.</li>
 *   <li>No setter annotations, no domain mutation methods.</li>
 *   <li>No BaseEntity — does NOT need updatedAt because it never changes.</li>
 * </ul>
 *
 * <p>pgPayload is stored as JSONB via {@link JsonNode} (Jackson-aware, type-safe)
 * using Hibernate's {@code @JdbcTypeCode(SqlTypes.JSON)}.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_transactions_transaction_id", columnList = "transaction_id", unique = true),
        @Index(name = "idx_payment_transactions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_payment_transactions_idempotency_key", columnList = "idempotency_key"),
        @Index(name = "idx_payment_transactions_owner", columnList = "owner_entity, owner_id"),
        @Index(name = "idx_payment_transactions_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private TransactionDirection direction;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    /** Domain entity name owning this transaction (e.g. "Invoice", "AlimtalkCredit"). */
    @Column(name = "owner_entity", length = 50)
    private String ownerEntity;

    @Column(name = "owner_id")
    private Long ownerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pg_payload", columnDefinition = "jsonb")
    private JsonNode pgPayload;

    @Column(name = "http_status")
    private Integer httpStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private PaymentTransaction(String transactionId, Long tenantId,
                                TransactionType type, TransactionDirection direction,
                                String idempotencyKey, String ownerEntity, Long ownerId,
                                JsonNode pgPayload, Integer httpStatus) {
        this.transactionId = transactionId;
        this.tenantId = tenantId;
        this.type = type;
        this.direction = direction;
        this.idempotencyKey = idempotencyKey;
        this.ownerEntity = ownerEntity;
        this.ownerId = ownerId;
        this.pgPayload = pgPayload;
        this.httpStatus = httpStatus;
    }

    // ─── equals / hashCode (ID-based) ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentTransaction other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
