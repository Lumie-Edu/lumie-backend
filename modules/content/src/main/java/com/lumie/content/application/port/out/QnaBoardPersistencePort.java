package com.lumie.content.application.port.out;

import com.lumie.content.domain.entity.QnaBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QnaBoardPersistencePort {
    Optional<QnaBoard> findByIdWithComments(Long id);
    Optional<QnaBoard> findById(Long id);
    Page<QnaBoard> findByQnaUserId(Long userId, Pageable pageable);
    Page<QnaBoard> findByIsAnsweredFalse(Pageable pageable);
    Page<QnaBoard> findByIsAnswered(Boolean answered, Pageable pageable);
    long countByIsAnsweredFalseAndCreatedAtBefore(Instant threshold);
    Page<QnaBoard> search(String keyword, List<Long> userIds, Boolean answered, Pageable pageable);
    Page<QnaBoard> searchByUser(Long userId, String keyword, Boolean answered, Pageable pageable);
    QnaBoard save(QnaBoard board);
    void flush();
    void delete(QnaBoard board);
}
