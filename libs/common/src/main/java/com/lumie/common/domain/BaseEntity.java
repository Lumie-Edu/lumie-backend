package com.lumie.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
// Spring Data auditing is the sole intentional Spring dependency in this
// shared infrastructure layer. @CreatedDate / @LastModifiedDate are pure
// metadata annotations (no bean-wiring semantics); AuditingEntityListener
// is the accepted bridge. Do not generalize this exception to other
// Spring annotations in domain code.
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Audit-timestamp base class for tenant-scoped and platform entities.
 *
 * <p>{@code createdAt} / {@code updatedAt} are populated by Spring Data JPA
 * Auditing (see {@code JpaAuditingConfig}). The custom auditing
 * {@code DateTimeProvider} returns {@link Instant#now()} to preserve UTC
 * parity with the previous manual {@code @PrePersist}/{@code @PreUpdate}
 * implementation.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
