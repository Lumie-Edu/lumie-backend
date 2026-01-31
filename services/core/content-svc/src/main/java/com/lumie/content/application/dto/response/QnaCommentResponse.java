package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaComment;
import com.lumie.content.domain.vo.AuthorType;

import java.time.LocalDateTime;

public record QnaCommentResponse(
        Long id,
        Long qnaBoardId,
        String content,
        Long authorId,
        String authorName,
        AuthorType authorType,
        LocalDateTime createdAt
) {
    public static QnaCommentResponse from(QnaComment comment) {
        return new QnaCommentResponse(
                comment.getId(),
                comment.getQnaBoard().getId(),
                comment.getContent(),
                comment.getAuthorId(),
                comment.getAuthorName(),
                comment.getAuthorType(),
                comment.getCreatedAt()
        );
    }
}
