package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.QnaComment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QnaDetailResponse(
        Long id,
        @Schema(nullable = true) Long qnaUserId,
        String qnaUserName,
        String qnaTitle,
        String qnaContent,
        Boolean isAnswered,
        List<QnaCommentResponse> comments,
        Instant createdAt,
        Instant updatedAt
) {
    private static final String DELETED_STAFF_NAME = "삭제된 선생님";

    public static QnaDetailResponse from(QnaBoard qnaBoard, String qnaUserName, Map<Long, String> staffNames) {
        return from(qnaBoard, qnaUserName, staffNames, DELETED_STAFF_NAME);
    }

    public static QnaDetailResponse from(
            QnaBoard qnaBoard,
            String qnaUserName,
            Map<Long, String> staffNames,
            String deletedStaffName) {
        List<QnaCommentResponse> commentResponses = qnaBoard.getComments().stream()
                .map(comment -> QnaCommentResponse.from(comment, resolveStaffName(comment, staffNames, deletedStaffName)))
                .toList();

        return new QnaDetailResponse(
                qnaBoard.getId(),
                qnaBoard.getQnaUserId(),
                qnaUserName,
                qnaBoard.getQnaTitle(),
                qnaBoard.getQnaContent(),
                qnaBoard.getIsAnswered(),
                commentResponses,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }

    private static String resolveStaffName(QnaComment comment, Map<Long, String> staffNames, String deletedStaffName) {
        if (comment.getStudentId() != null) {
            return null;
        }
        if (comment.getUserId() == null) {
            return deletedStaffName;
        }
        return staffNames.getOrDefault(comment.getUserId(), deletedStaffName);
    }
}
