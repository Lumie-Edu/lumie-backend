package com.lumie.common.domain;

import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

/**
 * Base class for tenant-scoped JPA entities. Adds a {@code tenant_id}
 * column that is auto-populated from {@link TenantContextHolder} on
 * insert, and is the column Postgres RLS policies filter against via
 * {@code current_setting('app.tenant_id')} (bound per-tx by
 * {@link com.lumie.common.tenant.RlsTenantContextAspect}).
 *
 * <p>The column is {@code nullable = false, updatable = false} — a row's
 * tenant is set once at creation and cannot move between tenants.
 * Cross-tenant data movement is a destructive admin operation that must
 * go through raw SQL with explicit RLS bypass, not entity mutation.
 *
 * <p>Entities that should be tenant-scoped extend this class; platform-
 * level entities (e.g. {@code tenants}, {@code plans},
 * {@code paddle_webhook_events}) continue to extend {@link BaseEntity}
 * directly and have no {@code tenant_id} column.
 *
 * <p>{@link PrePersist} callbacks fire superclass-first per JPA spec, so
 * {@link BaseEntity#onCreate()} runs before {@link #initTenantId()};
 * audit timestamps are populated by the time tenant binding executes.
 */
@Getter
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @PrePersist
    private void initTenantId() {
        if (tenantId == null) {
            tenantId = TenantContextHolder.getRequiredTenantId();
        }
    }
}
