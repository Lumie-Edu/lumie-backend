package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.QnaBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface QnaBoardRepository {

    QnaBoard save(QnaBoard qnaBoard);

    Optional<QnaBoard> findById(Long id);

    Optional<QnaBoard> findByIdWithComments(Long id);

    Page<QnaBoard> findAll(Pageable pageable);

    Page<QnaBoard> findByAuthorId(Long authorId, Pageable pageable);

    Page<QnaBoard> findByIsAnsweredFalse(Pageable pageable);

    void deleteById(Long id);

    boolean existsById(Long id);
}
