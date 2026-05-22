package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record QnaBoardResponse(
        Long id,
        Long qnaUserId,
        @Schema(nullable = true) String qnaUserName,
        String qnaTitle,
        Boolean isAnswered,
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
                qnaBoard.getIsAnswered(),
                qnaBoard.getComments() != null ? qnaBoard.getComments().size() : 0,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
