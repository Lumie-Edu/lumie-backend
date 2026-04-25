package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository {

    FileMetadata save(FileMetadata fileMetadata);

    Optional<FileMetadata> findById(UUID id);

    List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    List<FileMetadata> findByEntityTypeAndFolderId(EntityType entityType, UUID folderId);

    void deleteById(UUID id);

    boolean existsById(UUID id);

    boolean existsByFolderId(UUID folderId);
}
