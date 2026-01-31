package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.QnaComment;
import com.lumie.content.domain.repository.QnaCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QnaCommentRepositoryAdapter implements QnaCommentRepository {

    private final JpaQnaCommentRepository jpaRepository;

    @Override
    public QnaComment save(QnaComment qnaComment) {
        return jpaRepository.save(qnaComment);
    }

    @Override
    public Optional<QnaComment> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<QnaComment> findByQnaBoardId(Long qnaBoardId) {
        return jpaRepository.findByQnaBoardId(qnaBoardId);
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
