package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.ExamTemplatePersistencePort;
import com.lumie.exam.domain.entity.ExamTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExamTemplatePersistenceAdapter implements ExamTemplatePersistencePort {

    private final ExamTemplateJpaRepository jpaRepository;

    @Override public Optional<ExamTemplate> findById(Long id) { return jpaRepository.findById(id); }
    @Override public List<ExamTemplate> findAll() { return jpaRepository.findAll(); }
    @Override public Page<ExamTemplate> findAll(Pageable pageable) { return jpaRepository.findAll(pageable); }
    @Override public boolean existsById(Long id) { return jpaRepository.existsById(id); }
    @Override public ExamTemplate save(ExamTemplate template) { return jpaRepository.save(template); }
    @Override public void deleteById(Long id) { jpaRepository.deleteById(id); }
}
