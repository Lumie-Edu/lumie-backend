package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FileMetadataRepositoryAdapter implements FileMetadataRepository {

    private final JpaFileMetadataRepository jpaRepository;

    @Override
    public FileMetadata save(FileMetadata fileMetadata) {
        return jpaRepository.save(fileMetadata);
    }

    @Override
    public Optional<FileMetadata> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId) {
        return jpaRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public List<FileMetadata> findByEntityTypeAndFolderId(EntityType entityType, UUID folderId) {
        if (folderId == null) {
            return jpaRepository.findByEntityTypeAndFolderIdIsNull(entityType);
        }
        return jpaRepository.findByEntityTypeAndFolderId(entityType, folderId);
    }

    @Override
    public List<FileMetadata> findByEntityType(EntityType entityType) {
        return jpaRepository.findByEntityType(entityType);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByFolderId(UUID folderId) {
        return jpaRepository.existsByFolderId(folderId);
    }
}
