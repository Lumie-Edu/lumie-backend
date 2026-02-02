package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;

import java.time.LocalDateTime;
import java.util.List;

public record QnaDetailResponse(
        Long id,
        Long qnaUserId,
        String qnaTitle,
        String qnaContent,
        Boolean isItAnswered,
        List<QnaCommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QnaDetailResponse from(QnaBoard qnaBoard) {
        List<QnaCommentResponse> commentResponses = qnaBoard.getComments().stream()
                .map(QnaCommentResponse::from)
                .toList();

        return new QnaDetailResponse(
                qnaBoard.getId(),
                qnaBoard.getQnaUserId(),
                qnaBoard.getQnaTitle(),
                qnaBoard.getQnaContent(),
                qnaBoard.getIsItAnswered(),
                commentResponses,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
