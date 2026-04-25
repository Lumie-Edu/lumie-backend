package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record QnaDetailResponse(
        Long id,
        Long qnaUserId,
        String qnaUserName,
        String qnaTitle,
        String qnaContent,
        Boolean isItAnswered,
        List<QnaCommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QnaDetailResponse from(QnaBoard qnaBoard, String qnaUserName, Map<Long, String> adminNames) {
        List<QnaCommentResponse> commentResponses = qnaBoard.getComments().stream()
                .map(comment -> QnaCommentResponse.from(
                        comment,
                        comment.getUserId() != null ? adminNames.getOrDefault(comment.getUserId(), null) : null
                ))
                .toList();

        return new QnaDetailResponse(
                qnaBoard.getId(),
                qnaBoard.getQnaUserId(),
                qnaUserName,
                qnaBoard.getQnaTitle(),
                qnaBoard.getQnaContent(),
                qnaBoard.getIsItAnswered(),
                commentResponses,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
