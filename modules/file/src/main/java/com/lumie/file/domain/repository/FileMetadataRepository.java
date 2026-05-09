package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    List<FileMetadata> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    /**
     * Null-aware: returns rows where folderId IS NULL when {@code folderId} is null,
     * otherwise rows where folderId equals the supplied value.
     */
    @Query("""
        SELECT f FROM FileMetadata f
        WHERE f.entityType = :entityType
          AND ((:folderId IS NULL AND f.folderId IS NULL) OR f.folderId = :folderId)
        """)
    List<FileMetadata> findByEntityTypeAndFolderId(@Param("entityType") EntityType entityType,
                                                   @Param("folderId") UUID folderId);

    List<FileMetadata> findByEntityType(EntityType entityType);

    boolean existsByFolderId(UUID folderId);
}
