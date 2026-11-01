package com.lumie.file.application.port.out;

import com.lumie.file.domain.entity.TextbookFolder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TextbookFolderPersistencePort {
    List<TextbookFolder> findByParentId(UUID parentId);

    boolean existsByParentIdAndName(UUID parentId, String name);

    boolean existsByParentId(UUID parentId);

    Optional<TextbookFolder> findById(UUID id);

    TextbookFolder save(TextbookFolder folder);

    void deleteById(UUID id);
}
