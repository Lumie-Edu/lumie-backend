package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileMetadataJpaRepository extends JpaRepository<FileMetadata, UUID> {
    List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    @Query("""
        SELECT f FROM FileMetadata f
        WHERE f.entityType = :entityType
          AND ((:folderId IS NULL AND f.folderId IS NULL) OR f.folderId = :folderId)
        ORDER BY f.createdAt DESC, f.originalFilename ASC, f.id DESC
        """)
    List<FileMetadata> findByEntityTypeAndFolderId(@Param("entityType") EntityType entityType,
                                                   @Param("folderId") UUID folderId);

    @Query("""
        SELECT f FROM FileMetadata f
        WHERE f.entityType = :entityType
        ORDER BY f.createdAt DESC, f.originalFilename ASC, f.id DESC
        """)
    List<FileMetadata> findByEntityType(@Param("entityType") EntityType entityType);

    boolean existsByFolderId(UUID folderId);
}
