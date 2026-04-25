package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.TextbookFolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TextbookFolderRepository {

    TextbookFolder save(TextbookFolder folder);

    Optional<TextbookFolder> findById(UUID id);

    List<TextbookFolder> findByParentId(UUID parentId);

    void deleteById(UUID id);

    boolean existsByParentIdAndName(UUID parentId, String name);

    boolean existsByParentId(UUID parentId);
}
