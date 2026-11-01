package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.TextbookFolder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TextbookFolderJpaRepository extends JpaRepository<TextbookFolder, UUID> {
    @Query("""
        SELECT f FROM TextbookFolder f
        WHERE (:parentId IS NULL AND f.parentId IS NULL) OR f.parentId = :parentId
        ORDER BY f.name ASC
        """)
    List<TextbookFolder> findByParentId(@Param("parentId") UUID parentId);

    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM TextbookFolder f
        WHERE ((:parentId IS NULL AND f.parentId IS NULL) OR f.parentId = :parentId)
          AND f.name = :name
        """)
    boolean existsByParentIdAndName(@Param("parentId") UUID parentId, @Param("name") String name);

    boolean existsByParentId(UUID parentId);
}
