package com.lumie.file.adapter.out.persistence;

import com.lumie.file.application.port.out.FileMetadataPersistencePort;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FileMetadataPersistenceAdapter implements FileMetadataPersistencePort {
    private final FileMetadataJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId) {
        return repository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> findByEntityTypeAndFolderId(EntityType entityType, UUID folderId) {
        return repository.findByEntityTypeAndFolderId(entityType, folderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> findByEntityType(EntityType entityType) {
        return repository.findByEntityType(entityType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByFolderId(UUID folderId) {
        return repository.existsByFolderId(folderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileMetadata> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public FileMetadata save(FileMetadata metadata) {
        return repository.save(metadata);
    }

    @Override
    @Transactional
    public void flush() {
        repository.flush();
    }

    @Override
    @Transactional
    public void delete(FileMetadata metadata) {
        repository.delete(metadata);
    }
}
