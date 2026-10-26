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
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.api.FileService;
import com.lumie.file.api.PreparedTemporaryFile;
import com.lumie.file.api.PreparedUploadSession;
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
        requireCurrentStudent();
        rejectMixedUploadContracts(request.temporaryAttachments(), request.uploadSessionIds());
        return createForCurrentUser(request);
    }

    @Transactional
    public QnaBoardResponse updateMyQna(Long id, UpdateQnaRequest request) {
        QnaBoard qnaBoard = qnaBoardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));
        requireCurrentStudentOwns(qnaBoard);
        return updateQna(qnaBoard, id, request);
    }

    @Transactional
    public void deleteMyQna(Long id) {
        QnaBoard qnaBoard = qnaBoardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));
        requireCurrentStudentOwns(qnaBoard);
        deleteQna(qnaBoard, id);
    }

    private QnaBoardResponse createForCurrentUser(CreateQnaRequest request) {
        log.info("Creating Q&A: {}", request.qnaTitle());
        List<PreparedTemporaryFile> preparedFiles = fileService.prepareTemporaryFiles("qna", request.temporaryAttachments());
        List<PreparedUploadSession> preparedSessions = fileService.prepareUploadSessions("qna", request.uploadSessionIds());

        try {
        QnaBoard qnaBoard = QnaBoard.create(
                UserContextHolder.getRequiredUserId(),
                request.qnaTitle(),
                request.qnaContent()
        );

        QnaBoard saved = qnaBoardRepository.save(qnaBoard);
        fileService.attachPreparedFiles("qna", saved.getId(), preparedFiles);
        fileService.attachUploadSessions("qna", saved.getId(), preparedSessions, null);
        qnaBoardRepository.flush();
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
        rejectMixedUploadContracts(request.temporaryAttachments(), request.uploadSessionIds());
        QnaBoard qnaBoard = qnaBoardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));
        requireStudentOwnerOrStaff(qnaBoard);
        return updateQna(qnaBoard, id, request);
    }

    private QnaBoardResponse updateQna(QnaBoard qnaBoard, Long id, UpdateQnaRequest request) {
        List<PreparedTemporaryFile> preparedFiles = fileService.prepareTemporaryFiles("qna", request.temporaryAttachments());
        List<PreparedUploadSession> preparedSessions = fileService.prepareUploadSessions("qna", request.uploadSessionIds());

        try {
        qnaBoard.update(request.qnaTitle(), request.qnaContent());

        if (request.uploadSessionIds() != null) {
            fileService.reconcileUploadSessions("qna", id, request.attachmentFileIds(), preparedSessions);
        } else {
            fileService.reconcilePreparedDirectFiles("qna", id, request.attachmentFileIds(), preparedFiles);
        }

        QnaBoard updated = qnaBoardRepository.save(qnaBoard);
        qnaBoardRepository.flush();
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

        QnaBoard qnaBoard = qnaBoardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));
        requireStudentOwnerOrStaff(qnaBoard);
        deleteQna(qnaBoard, id);
    }

    private void deleteQna(QnaBoard qnaBoard, Long id) {
        fileService.deleteFilesByEntity("qna", id);
        qnaBoardRepository.delete(qnaBoard);
        log.info("Q&A deleted: {}", id);
    }

    @Transactional
    public QnaCommentResponse addComment(Long qnaId, CreateCommentRequest request) {
        AuthorizationGuard.requireStaff();
        log.info("Adding comment to Q&A: {}", qnaId);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(qnaId)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));

        QnaComment comment = QnaComment.createByStaff(
                qnaBoard, UserContextHolder.getRequiredUserId(), request.commentContent());

        qnaBoard.addComment(comment);
        QnaComment saved = qnaCommentRepository.save(comment);
        qnaBoardRepository.save(qnaBoard);

        log.info("Comment added with id: {}", saved.getId());
        return QnaCommentResponse.from(saved, null);
    }

    @Transactional
    public void deleteComment(Long qnaId, Long commentId) {
        AuthorizationGuard.requireStaff();
        log.info("Deleting comment {} from Q&A: {}", commentId, qnaId);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(qnaId)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));

        QnaComment comment = qnaBoard.getComments().stream()
                .filter(candidate -> candidate.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ContentErrorCode.COMMENT_NOT_FOUND));

        qnaCommentRepository.delete(comment);
        qnaBoard.getComments().remove(comment);

        if (!qnaBoard.hasStaffComment()) {
            qnaBoard.markAsUnanswered();
            qnaBoardRepository.save(qnaBoard);
        }

        log.info("Comment deleted: {}", commentId);
    }

    private void requireStudentOwnerOrStaff(QnaBoard qna) {
        Role role = UserContextHolder.getUserRole();
        if (role != Role.STUDENT) {
            AuthorizationGuard.requireStaff();
            return;
        }
        requireCurrentStudentOwns(qna);
    }

    private void requireCurrentStudent() {
        if (UserContextHolder.getUserRole() != Role.STUDENT) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        UserContextHolder.getRequiredUserId();
    }

    private void rejectMixedUploadContracts(Object temporary, Object sessions) {
        if (temporary != null && sessions != null) throw new BusinessException(CommonErrorCode.INVALID_INPUT);
    }

    private void requireCurrentStudentOwns(QnaBoard qna) {
        Long userId = UserContextHolder.getRequiredUserId();
        if (!userId.equals(qna.getQnaUserId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

}
