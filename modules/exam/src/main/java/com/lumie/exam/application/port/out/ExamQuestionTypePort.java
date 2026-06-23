package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.ExamQuestionType;

import java.util.Collection;
import java.util.List;

public interface ExamQuestionTypePort {

    List<ExamQuestionType> findAllOrdered();

    List<ExamQuestionType> findAllByNameIn(Collection<String> names);

    boolean existsByName(String name);

    int findMaxDisplayOrder();

    ExamQuestionType save(ExamQuestionType questionType);

    List<ExamQuestionType> saveAll(List<ExamQuestionType> questionTypes);

    boolean deleteById(Long id);
}
