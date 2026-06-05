package com.lumie.file.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.file.domain.vo.EntityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "file_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_file_links_file_entity",
                columnNames = {"tenant_id", "file_id", "entity_type", "entity_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class FileLink extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    private FileLink(FileMetadata file, EntityType entityType, Long entityId) {
        this.file = file;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public static FileLink create(FileMetadata file, EntityType entityType, Long entityId) {
        return new FileLink(file, entityType, entityId);
    }
}
