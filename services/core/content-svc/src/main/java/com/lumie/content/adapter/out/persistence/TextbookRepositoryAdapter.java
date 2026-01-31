package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.repository.TextbookRepository;
import com.lumie.content.domain.vo.TextbookStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TextbookRepositoryAdapter implements TextbookRepository {

    private final JpaTextbookRepository jpaRepository;

    @Override
    public Textbook save(Textbook textbook) {
        return jpaRepository.save(textbook);
    }

    @Override
    public Optional<Textbook> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Textbook> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<Textbook> findByAcademyId(Long academyId, Pageable pageable) {
        return jpaRepository.findByAcademyId(academyId, pageable);
    }

    @Override
    public Page<Textbook> findBySubject(String subject, Pageable pageable) {
        return jpaRepository.findBySubject(subject, pageable);
    }

    @Override
    public List<Textbook> findByStatus(TextbookStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
