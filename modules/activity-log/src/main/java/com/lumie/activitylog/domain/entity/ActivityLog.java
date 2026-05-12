package com.lumie.activitylog.domain.entity;

import com.lumie.common.auth.ActionType;
import com.lumie.common.auth.EntityType;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "actor_name", nullable = false, length = 100)
    private String actorName;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "actor_role", nullable = false, columnDefinition = "actor_role_type")
    private Role actorRole;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action", nullable = false, columnDefinition = "activity_action_type")
    private ActionType action;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "entity_type", nullable = false, columnDefinition = "activity_entity_type")
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name", length = 200)
    private String entityName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.tenantId == null) {
            this.tenantId = TenantContextHolder.getRequiredTenantId();
        }
    }

    private ActivityLog(Long actorId, String actorName, Role actorRole,
                        ActionType action, EntityType entityType, Long entityId,
                        String entityName, String description) {
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityName = entityName;
        this.description = description;
    }

    public static ActivityLog create(Long actorId, String actorName, Role actorRole,
                                     ActionType action, EntityType entityType, Long entityId,
                                     String entityName, String description) {
        return new ActivityLog(actorId, actorName, actorRole, action, entityType, entityId, entityName, description);
    }
}
