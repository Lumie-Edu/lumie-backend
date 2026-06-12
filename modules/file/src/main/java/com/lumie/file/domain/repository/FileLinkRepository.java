package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.FileLink;
import com.lumie.file.domain.vo.EntityType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileLinkRepository extends JpaRepository<FileLink, UUID> {

    @EntityGraph(attributePaths = "file")
    List<FileLink> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    @EntityGraph(attributePaths = "file")
    List<FileLink> findByFile_Id(UUID fileId);

    void deleteByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    boolean existsByFile_Id(UUID fileId);
}
