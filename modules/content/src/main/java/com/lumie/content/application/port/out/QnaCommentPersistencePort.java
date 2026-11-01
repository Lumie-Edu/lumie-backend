package com.lumie.content.application.port.out;

import com.lumie.content.domain.entity.QnaComment;
import java.util.List;

public interface QnaCommentPersistencePort {
    List<QnaComment> findByQnaBoardId(Long boardId);
    QnaComment save(QnaComment comment);
    void delete(QnaComment comment);
}
