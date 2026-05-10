package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;

import java.time.Instant;

public record QnaBoardResponse(
        Long id,
        Long qnaUserId,
        String qnaUserName,
        String qnaTitle,
        Boolean isItAnswered,
        Integer commentCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static QnaBoardResponse from(QnaBoard qnaBoard, String qnaUserName) {
        return new QnaBoardResponse(
                qnaBoard.getId(),
                qnaBoard.getQnaUserId(),
                qnaUserName,
                qnaBoard.getQnaTitle(),
                qnaBoard.getIsItAnswered(),
                qnaBoard.getComments() != null ? qnaBoard.getComments().size() : 0,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
