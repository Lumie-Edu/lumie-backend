package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.repository.QnaBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QnaBoardRepositoryAdapter implements QnaBoardRepository {

    private final JpaQnaBoardRepository jpaRepository;

    @Override
    public QnaBoard save(QnaBoard qnaBoard) {
        return jpaRepository.save(qnaBoard);
    }

    @Override
    public Optional<QnaBoard> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<QnaBoard> findByIdWithComments(Long id) {
        return jpaRepository.findByIdWithComments(id);
    }

    @Override
    public Page<QnaBoard> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<QnaBoard> findByQnaUserId(Long qnaUserId, Pageable pageable) {
        return jpaRepository.findByQnaUserId(qnaUserId, pageable);
    }

    @Override
    public Page<QnaBoard> findByIsItAnsweredFalse(Pageable pageable) {
        return jpaRepository.findByIsItAnsweredFalse(pageable);
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
