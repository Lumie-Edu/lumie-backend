package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.QnaBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaQnaBoardRepository extends JpaRepository<QnaBoard, Long> {

    @Query("SELECT q FROM QnaBoard q LEFT JOIN FETCH q.comments WHERE q.id = :id")
    Optional<QnaBoard> findByIdWithComments(@Param("id") Long id);

    Page<QnaBoard> findByQnaUserId(Long qnaUserId, Pageable pageable);

    Page<QnaBoard> findByIsItAnsweredFalse(Pageable pageable);
}
