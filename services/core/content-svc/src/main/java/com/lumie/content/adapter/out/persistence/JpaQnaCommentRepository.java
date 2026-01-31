package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.QnaComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaQnaCommentRepository extends JpaRepository<QnaComment, Long> {

    List<QnaComment> findByQnaBoardId(Long qnaBoardId);
}
