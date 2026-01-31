package com.lumie.content.application.service;

import com.lumie.content.application.dto.request.CreateCommentRequest;
import com.lumie.content.application.dto.request.CreateQnaRequest;
import com.lumie.content.application.dto.request.UpdateQnaRequest;
import com.lumie.content.application.dto.response.QnaBoardResponse;
import com.lumie.content.application.dto.response.QnaCommentResponse;
import com.lumie.content.application.port.out.ContentEventPublisherPort;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.QnaComment;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.QnaBoardRepository;
import com.lumie.content.domain.repository.QnaCommentRepository;
import com.lumie.content.domain.vo.AuthorType;
import com.lumie.content.infrastructure.tenant.TenantContextHolder;
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
    private final ContentEventPublisherPort eventPublisher;

    @Transactional
    public QnaBoardResponse createQna(CreateQnaRequest request) {
        log.info("Creating Q&A: {}", request.title());

        QnaBoard qnaBoard = QnaBoard.create(
                request.title(),
                request.content(),
                request.studentId(),
                request.studentName()
        );

        QnaBoard saved = qnaBoardRepository.save(qnaBoard);
        log.info("Q&A created with id: {}", saved.getId());

        return QnaBoardResponse.from(saved);
    }

    @Transactional
    public QnaBoardResponse updateQna(Long id, UpdateQnaRequest request) {
        log.info("Updating Q&A: {}", id);

        QnaBoard qnaBoard = qnaBoardRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.QNA_NOT_FOUND));

        qnaBoard.update(request.title(), request.content());

        QnaBoard updated = qnaBoardRepository.save(qnaBoard);
        log.info("Q&A updated: {}", updated.getId());

        return QnaBoardResponse.from(updated);
    }

    @Transactional
    public void deleteQna(Long id) {
        log.info("Deleting Q&A: {}", id);

        if (!qnaBoardRepository.existsById(id)) {
            throw new ContentException(ContentErrorCode.QNA_NOT_FOUND);
        }

        qnaBoardRepository.deleteById(id);
        log.info("Q&A deleted: {}", id);
    }

    @Transactional
    public QnaCommentResponse addComment(Long qnaId, CreateCommentRequest request) {
        log.info("Adding comment to Q&A: {}", qnaId);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(qnaId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.QNA_NOT_FOUND));

        boolean hadAdminComment = qnaBoard.hasAdminComment();

        QnaComment comment;
        if (request.authorType() == AuthorType.ADMIN) {
            comment = QnaComment.createFromAdmin(
                    qnaBoard,
                    request.content(),
                    request.authorId(),
                    request.authorName()
            );
        } else {
            comment = QnaComment.createFromStudent(
                    qnaBoard,
                    request.content(),
                    request.authorId(),
                    request.authorName()
            );
        }

        qnaBoard.addComment(comment);
        QnaComment saved = qnaCommentRepository.save(comment);
        qnaBoardRepository.save(qnaBoard);

        // Publish event if this is the first admin comment
        if (!hadAdminComment && comment.isFromAdmin()) {
            String tenantSlug = TenantContextHolder.getTenantSlug();
            eventPublisher.publishQnaReplied(qnaBoard, tenantSlug);
        }

        log.info("Comment added with id: {}", saved.getId());
        return QnaCommentResponse.from(saved);
    }

    @Transactional
    public void deleteComment(Long qnaId, Long commentId) {
        log.info("Deleting comment {} from Q&A: {}", commentId, qnaId);

        if (!qnaBoardRepository.existsById(qnaId)) {
            throw new ContentException(ContentErrorCode.QNA_NOT_FOUND);
        }

        if (!qnaCommentRepository.existsById(commentId)) {
            throw new ContentException(ContentErrorCode.COMMENT_NOT_FOUND);
        }

        qnaCommentRepository.deleteById(commentId);
        log.info("Comment deleted: {}", commentId);
    }
}
