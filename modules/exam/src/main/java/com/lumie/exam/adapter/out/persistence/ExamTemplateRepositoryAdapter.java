package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamTemplate;
import com.lumie.exam.domain.repository.ExamTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExamTemplateRepositoryAdapter implements ExamTemplateRepository {

    private final JpaExamTemplateRepository jpaRepository;

    @Override
    public ExamTemplate save(ExamTemplate template) {
        return jpaRepository.save(template);
    }

    @Override
    public Optional<ExamTemplate> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<ExamTemplate> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
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
