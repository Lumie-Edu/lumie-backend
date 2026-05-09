package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.QnaComment;

import java.util.List;
import java.util.Optional;

public interface QnaCommentRepository {

    QnaComment save(QnaComment qnaComment);

    Optional<QnaComment> findById(Long id);

    List<QnaComment> findByQnaBoardId(Long qnaBoardId);

    void deleteById(Long id);

    boolean existsById(Long id);
}
