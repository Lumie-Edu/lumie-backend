package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.ExamQuestionTypePort;
import com.lumie.exam.domain.entity.ExamQuestionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
class ExamQuestionTypePersistenceAdapter implements ExamQuestionTypePort {

    private final ExamQuestionTypeJpaRepository repository;

    @Override
    public List<ExamQuestionType> findAllOrdered() {
        return repository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Override
    public List<ExamQuestionType> findAllByNameIn(Collection<String> names) {
        return repository.findAllByNameIn(names);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public int findMaxDisplayOrder() {
        return repository.findMaxDisplayOrder();
    }

    @Override
    public ExamQuestionType save(ExamQuestionType questionType) {
        return repository.save(questionType);
    }

    @Override
    public List<ExamQuestionType> saveAll(List<ExamQuestionType> questionTypes) {
        return repository.saveAll(questionTypes);
    }

    @Override
    public boolean deleteById(Long id) {
        return repository.deleteByIdReturningCount(id) > 0;
    }
}
