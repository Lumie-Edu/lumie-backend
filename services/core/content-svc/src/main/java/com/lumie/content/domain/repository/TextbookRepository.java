package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.vo.TextbookCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TextbookRepository {

    Textbook save(Textbook textbook);

    Optional<Textbook> findById(Long id);

    Page<Textbook> findAll(Pageable pageable);

    Page<Textbook> findByCategory(TextbookCategory category, Pageable pageable);

    List<Textbook> findByIsImportantTrue();

    void deleteById(Long id);

    boolean existsById(Long id);
}
