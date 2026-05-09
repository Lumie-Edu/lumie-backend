package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.QnaComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnaCommentRepository extends JpaRepository<QnaComment, Long> {

    List<QnaComment> findByQnaBoardId(Long qnaBoardId);
}
