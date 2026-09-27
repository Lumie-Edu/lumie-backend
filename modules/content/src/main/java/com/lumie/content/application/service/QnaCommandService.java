package com.lumie.content.application.service;

import com.lumie.content.application.dto.request.CreateCommentRequest;
import com.lumie.content.application.dto.request.CreateQnaRequest;
import com.lumie.content.application.dto.request.UpdateQnaRequest;
import com.lumie.content.application.dto.response.QnaBoardResponse;
import com.lumie.content.application.dto.response.QnaCommentResponse;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.QnaComment;
import com.lumie.common.exception.BusinessException;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.repository.QnaBoardRepository;
import com.lumie.content.domain.repository.QnaCommentRepository;
import com.lumie.file.api.FileService;
import com.lumie.file.api.PreparedTemporaryFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        List<PreparedTemporaryFile> preparedFiles = fileService.prepareTemporaryFiles("qna", request.temporaryAttachments());

        try {
        QnaBoard qnaBoard = QnaBoard.create(
                request.qnaUserId(),
                request.qnaTitle(),
                request.qnaContent()
        );

        QnaBoard saved = qnaBoardRepository.save(qnaBoard);
        fileService.attachPreparedFiles("qna", saved.getId(), preparedFiles);
        log.info("Q&A created with id: {}", saved.getId());

        return QnaBoardResponse.from(saved, null);
        } catch (RuntimeException exception) {
            fileService.discardPreparedFiles(preparedFiles);
            throw exception;
        }
    }

    @Transactional
    public QnaBoardResponse updateQna(Long id, UpdateQnaRequest request) {
        log.info("Updating Q&A: {}", id);
        List<PreparedTemporaryFile> preparedFiles = fileService.prepareTemporaryFiles("qna", request.temporaryAttachments());

        try {
        QnaBoard qnaBoard = qnaBoardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));

        qnaBoard.update(request.qnaTitle(), request.qnaContent());

        fileService.reconcilePreparedDirectFiles("qna", id, request.attachmentFileIds(), preparedFiles);

        QnaBoard updated = qnaBoardRepository.save(qnaBoard);
        log.info("Q&A updated: {}", updated.getId());

        return QnaBoardResponse.from(updated, null);
        } catch (RuntimeException exception) {
            fileService.discardPreparedFiles(preparedFiles);
            throw exception;
        }
    }

    @Transactional
    public void deleteQna(Long id) {
        log.info("Deleting Q&A: {}", id);

        if (!qnaBoardRepository.existsById(id)) {
            throw new BusinessException(ContentErrorCode.QNA_NOT_FOUND);
        }

        fileService.deleteFilesByEntity("qna", id);
        qnaBoardRepository.deleteById(id);
        log.info("Q&A deleted: {}", id);
    }

    @Transactional
    public QnaCommentResponse addComment(Long qnaId, CreateCommentRequest request) {
        log.info("Adding comment to Q&A: {}", qnaId);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(qnaId)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));

        if (!qnaCommentRepository.existsById(commentId)) {
            throw new BusinessException(ContentErrorCode.COMMENT_NOT_FOUND);
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
