package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.TextbookFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TextbookFolderRepository extends JpaRepository<TextbookFolder, UUID> {

    /**
     * Null-aware: returns root-level folders (parentId IS NULL) when {@code parentId}
     * is null, otherwise children of the given parent. Ordered by name ASC.
     */
    @Query("""
        SELECT f FROM TextbookFolder f
        WHERE (:parentId IS NULL AND f.parentId IS NULL) OR f.parentId = :parentId
        ORDER BY f.name ASC
        """)
    List<TextbookFolder> findByParentId(@Param("parentId") UUID parentId);

    /**
     * Null-aware existence check, mirrors {@link #findByParentId}.
     */
    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM TextbookFolder f
        WHERE ((:parentId IS NULL AND f.parentId IS NULL) OR f.parentId = :parentId)
          AND f.name = :name
        """)
    boolean existsByParentIdAndName(@Param("parentId") UUID parentId, @Param("name") String name);

    boolean existsByParentId(UUID parentId);
}
