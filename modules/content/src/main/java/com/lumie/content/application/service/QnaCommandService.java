package com.lumie.content.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.application.dto.request.CreateCommentRequest;
import com.lumie.content.application.dto.request.CreateQnaRequest;
import com.lumie.content.application.dto.request.UpdateQnaRequest;
import com.lumie.content.application.dto.response.QnaBoardResponse;
import com.lumie.content.application.dto.response.QnaCommentResponse;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.QnaComment;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.QnaBoardRepository;
import com.lumie.content.domain.repository.QnaCommentRepository;
import com.lumie.internal.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QnaCommandService {

    private final QnaBoardRepository qnaBoardRepository;
    private final QnaCommentRepository qnaCommentRepository;
    private final FileService fileService;

    @Transactional
    public QnaBoardResponse createQna(CreateQnaRequest request) {
        log.info("Creating Q&A: {}", request.qnaTitle());

        QnaBoard qnaBoard = QnaBoard.create(
                request.qnaUserId(),
                request.qnaTitle(),
                request.qnaContent()
        );

        QnaBoard saved = qnaBoardRepository.save(qnaBoard);
        log.info("Q&A created with id: {}", saved.getId());

        return QnaBoardResponse.from(saved, null);
    }

    @Transactional
    public QnaBoardResponse updateQna(Long id, UpdateQnaRequest request) {
        log.info("Updating Q&A: {}", id);

        QnaBoard qnaBoard = qnaBoardRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.QNA_NOT_FOUND));

        qnaBoard.update(request.qnaTitle(), request.qnaContent());

        if (request.fileIdsToDelete() != null && !request.fileIdsToDelete().isEmpty()) {
            fileService.deleteFilesByIds(request.fileIdsToDelete());
        }

        QnaBoard updated = qnaBoardRepository.save(qnaBoard);
        log.info("Q&A updated: {}", updated.getId());

        return QnaBoardResponse.from(updated, null);
    }

    @Transactional
    public void deleteQna(Long id) {
        log.info("Deleting Q&A: {}", id);

        if (!qnaBoardRepository.existsById(id)) {
            throw new ContentException(ContentErrorCode.QNA_NOT_FOUND);
        }

        fileService.deleteFilesByEntity("qna", id);
        qnaBoardRepository.deleteById(id);
        log.info("Q&A deleted: {}", id);
    }

    @Transactional
    public QnaCommentResponse addComment(Long qnaId, CreateCommentRequest request) {
        log.info("Adding comment to Q&A: {}", qnaId);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(qnaId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.QNA_NOT_FOUND));

        QnaComment comment;
        if (request.userId() != null) {
            comment = QnaComment.createByAdmin(qnaBoard, request.userId(), request.commentContent());
        } else {
            comment = QnaComment.createByStudent(qnaBoard, request.studentId(), request.commentContent());
        }

        qnaBoard.addComment(comment);
        QnaComment saved = qnaCommentRepository.save(comment);
        qnaBoardRepository.save(qnaBoard);

        log.info("Comment added with id: {}", saved.getId());
        return QnaCommentResponse.from(saved, null);
    }

    @Transactional
    public void deleteComment(Long qnaId, Long commentId) {
        log.info("Deleting comment {} from Q&A: {}", commentId, qnaId);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(qnaId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.QNA_NOT_FOUND));

        if (!qnaCommentRepository.existsById(commentId)) {
            throw new ContentException(ContentErrorCode.COMMENT_NOT_FOUND);
        }

        qnaCommentRepository.deleteById(commentId);
        qnaBoard.getComments().removeIf(c -> c.getId().equals(commentId));

        boolean stillHasAdminComment = qnaBoard.getComments().stream()
                .anyMatch(c -> c.getUserId() != null);

        if (!stillHasAdminComment) {
            qnaBoard.markAsUnanswered();
            qnaBoardRepository.save(qnaBoard);
        }

        log.info("Comment deleted: {}", commentId);
    }
}
