package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository {

    FileMetadata save(FileMetadata fileMetadata);

    Optional<FileMetadata> findById(UUID id);

    Optional<FileMetadata> findByIdAndTenantSlug(UUID id, String tenantSlug);

    List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    List<FileMetadata> findByTenantSlugAndEntityTypeAndEntityId(String tenantSlug, EntityType entityType, Long entityId);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
