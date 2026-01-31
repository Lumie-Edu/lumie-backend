package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;

import java.time.LocalDateTime;

public record QnaBoardResponse(
        Long id,
        Long academyId,
        Long authorId,
        String title,
        String content,
        String category,
        Boolean isAnswered,
        Boolean isPrivate,
        Integer viewCount,
        Integer commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QnaBoardResponse from(QnaBoard qnaBoard) {
        return new QnaBoardResponse(
                qnaBoard.getId(),
                qnaBoard.getAcademyId(),
                qnaBoard.getAuthorId(),
                qnaBoard.getTitle(),
                qnaBoard.getContent(),
                qnaBoard.getCategory(),
                qnaBoard.getIsAnswered(),
                qnaBoard.getIsPrivate(),
                qnaBoard.getViewCount(),
                qnaBoard.getComments() != null ? qnaBoard.getComments().size() : 0,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
