package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaFileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    Optional<FileMetadata> findByIdAndTenantSlug(UUID id, String tenantSlug);

    List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    List<FileMetadata> findByTenantSlugAndEntityTypeAndEntityId(String tenantSlug, EntityType entityType, Long entityId);
}
