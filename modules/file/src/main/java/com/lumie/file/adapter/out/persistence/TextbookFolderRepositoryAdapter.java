package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.TextbookFolder;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TextbookFolderRepositoryAdapter implements TextbookFolderRepository {

    private final JpaTextbookFolderRepository jpaRepository;

    @Override
    public TextbookFolder save(TextbookFolder folder) {
        return jpaRepository.save(folder);
    }

    @Override
    public Optional<TextbookFolder> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<TextbookFolder> findByParentId(UUID parentId) {
        if (parentId == null) {
            return jpaRepository.findByParentIdIsNullOrderByNameAsc();
        }
        return jpaRepository.findByParentIdOrderByNameAsc(parentId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByParentIdAndName(UUID parentId, String name) {
        if (parentId == null) {
            return jpaRepository.existsByParentIdIsNullAndName(name);
        }
        return jpaRepository.existsByParentIdAndName(parentId, name);
    }

    @Override
    public boolean existsByParentId(UUID parentId) {
        return jpaRepository.existsByParentId(parentId);
    }
}
