package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.TextbookFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaTextbookFolderRepository extends JpaRepository<TextbookFolder, UUID> {

    List<TextbookFolder> findByParentIdOrderByNameAsc(UUID parentId);

    List<TextbookFolder> findByParentIdIsNullOrderByNameAsc();

    boolean existsByParentIdAndName(UUID parentId, String name);

    boolean existsByParentIdIsNullAndName(String name);

    boolean existsByParentId(UUID parentId);
}
