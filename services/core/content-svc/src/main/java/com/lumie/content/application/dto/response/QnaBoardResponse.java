package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;

import java.time.LocalDateTime;

public record QnaBoardResponse(
        Long id,
        Long qnaUserId,
        String qnaTitle,
        String qnaContent,
        Boolean isItAnswered,
        Integer commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QnaBoardResponse from(QnaBoard qnaBoard) {
        return new QnaBoardResponse(
                qnaBoard.getId(),
                qnaBoard.getQnaUserId(),
                qnaBoard.getQnaTitle(),
                qnaBoard.getQnaContent(),
                qnaBoard.getIsItAnswered(),
                qnaBoard.getComments() != null ? qnaBoard.getComments().size() : 0,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
