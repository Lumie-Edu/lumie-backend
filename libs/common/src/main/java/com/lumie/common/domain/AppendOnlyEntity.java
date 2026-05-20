package com.lumie.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

import java.time.Instant;

/**
 * Base class for append-only (insert-once, never mutated) JPA entities.
 *
 * <p>Provides a single {@code created_at TIMESTAMPTZ} column populated at
 * insert time via {@link PrePersist}. Intentionally omits {@code updated_at}:
 * append-only tables (e.g. {@code activity_logs}) are never updated, so an
 * update timestamp column would always be null or stale and would require a
 * schema column that carries no information.
 *
 * <p>Subclasses must manage their own {@code tenant_id} binding (either by
 * extending {@link TenantScopedEntity} instead, or by declaring the column
 * directly) because tenant scoping is orthogonal to the append-only contract.
 * {@code ActivityLog} extends this class and owns its own {@code tenant_id}
 * column to remain explicit about RLS column ownership.
 *
 * <p>Intentionally NOT migrated to Spring Data JPA Auditing (which
 * {@link BaseEntity} now uses): the append-only contract guarantees no
 * {@code @LastModifiedDate} path will ever fire, so the lighter manual
 * {@code @PrePersist} avoids importing the {@code AuditingEntityListener}
 * coupling for zero behavioral benefit.
 */
@Getter
@MappedSuperclass
public abstract class AppendOnlyEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
