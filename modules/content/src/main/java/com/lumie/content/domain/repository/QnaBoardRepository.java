package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.QnaBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QnaBoardRepository extends JpaRepository<QnaBoard, Long> {

    @EntityGraph(attributePaths = {"comments"})
    @Query("SELECT q FROM QnaBoard q WHERE q.id = :id")
    Optional<QnaBoard> findByIdWithComments(Long id);

    Page<QnaBoard> findByQnaUserId(Long qnaUserId, Pageable pageable);

    Page<QnaBoard> findByIsAnsweredFalse(Pageable pageable);

    Page<QnaBoard> findByIsAnswered(Boolean isAnswered, Pageable pageable);
}
