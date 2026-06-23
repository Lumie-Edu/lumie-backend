package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface ExamQuestionTypeJpaRepository extends JpaRepository<ExamQuestionType, Long> {

    List<ExamQuestionType> findAllByOrderByDisplayOrderAscNameAsc();

    List<ExamQuestionType> findAllByNameIn(Collection<String> names);

    boolean existsByName(String name);

    @Query("select coalesce(max(t.displayOrder), 0) from ExamQuestionType t")
    int findMaxDisplayOrder();

    @Modifying
    @Query("delete from ExamQuestionType t where t.id = :id")
    int deleteByIdReturningCount(@Param("id") Long id);
}
