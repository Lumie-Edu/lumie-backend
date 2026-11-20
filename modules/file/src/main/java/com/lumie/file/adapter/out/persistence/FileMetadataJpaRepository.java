package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataJpaRepository extends JpaRepository<FileMetadata, UUID> {
    List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

}
