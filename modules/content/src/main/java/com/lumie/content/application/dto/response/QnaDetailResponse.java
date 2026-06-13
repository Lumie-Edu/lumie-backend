package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.QnaComment;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QnaDetailResponse(
        Long id,
        Long qnaUserId,
        String qnaUserName,
        String qnaTitle,
        String qnaContent,
        Boolean isAnswered,
        List<QnaCommentResponse> comments,
        Instant createdAt,
        Instant updatedAt
) {
    private static final String DELETED_STAFF_NAME = "삭제된 직원";

    public static QnaDetailResponse from(QnaBoard qnaBoard, String qnaUserName, Map<Long, String> adminNames) {
        return from(qnaBoard, qnaUserName, adminNames, DELETED_STAFF_NAME);
    }

    public static QnaDetailResponse from(
            QnaBoard qnaBoard,
            String qnaUserName,
            Map<Long, String> adminNames,
            String deletedStaffName) {
        List<QnaCommentResponse> commentResponses = qnaBoard.getComments().stream()
                .map(comment -> QnaCommentResponse.from(comment, resolveAdminName(comment, adminNames, deletedStaffName)))
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

    private static String resolveAdminName(QnaComment comment, Map<Long, String> adminNames, String deletedStaffName) {
        if (comment.getStudentId() != null) {
            return null;
        }
        if (comment.getUserId() == null) {
            return deletedStaffName;
        }
        return adminNames.getOrDefault(comment.getUserId(), deletedStaffName);
    }
}
