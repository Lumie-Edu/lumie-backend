package com.lumie.file.application.port.out;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataPersistencePort {
    List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    List<FileMetadata> findByEntityTypeAndFolderId(EntityType entityType, UUID folderId);

    List<FileMetadata> findByEntityType(EntityType entityType);

    boolean existsByFolderId(UUID folderId);

    Optional<FileMetadata> findById(UUID id);

    FileMetadata save(FileMetadata metadata);

    void flush();

    void delete(FileMetadata metadata);
}
