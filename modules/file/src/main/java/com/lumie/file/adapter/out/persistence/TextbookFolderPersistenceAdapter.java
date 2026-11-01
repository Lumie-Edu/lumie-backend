package com.lumie.file.adapter.out.persistence;

import com.lumie.file.application.port.out.TextbookFolderPersistencePort;
import com.lumie.file.domain.entity.TextbookFolder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TextbookFolderPersistenceAdapter implements TextbookFolderPersistencePort {
    private final TextbookFolderJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<TextbookFolder> findByParentId(UUID parentId) {
        return repository.findByParentId(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByParentIdAndName(UUID parentId, String name) {
        return repository.existsByParentIdAndName(parentId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByParentId(UUID parentId) {
        return repository.existsByParentId(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TextbookFolder> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public TextbookFolder save(TextbookFolder folder) {
        return repository.save(folder);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
