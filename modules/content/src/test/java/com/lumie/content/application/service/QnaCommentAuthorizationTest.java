package com.lumie.content.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.application.dto.request.CreateCommentRequest;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.QnaComment;
import com.lumie.content.domain.repository.QnaBoardRepository;
import com.lumie.content.domain.repository.QnaCommentRepository;
import com.lumie.file.api.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class QnaCommentAuthorizationTest {

    private final QnaBoardRepository boards = mock(QnaBoardRepository.class);
    private final QnaCommentRepository comments = mock(QnaCommentRepository.class);
    private final QnaCommandService service = new QnaCommandService(boards, comments, mock(FileService.class));

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void studentCannotPostOrDeleteComments() {
        UserContextHolder.setUserId(10L);
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> service.addComment(1L, new CreateCommentRequest("답변")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.deleteComment(1L, 2L))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(boards, comments);
    }

    @Test
    void staffReplyUsesAuthenticatedUserIdAndMarksQuestionAnswered() {
        UserContextHolder.setUserId(42L);
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        QnaBoard board = board();
        given(boards.findByIdWithComments(1L)).willReturn(Optional.of(board));
        given(comments.save(any(QnaComment.class))).willAnswer(invocation -> invocation.getArgument(0));

        var response = service.addComment(1L, new CreateCommentRequest("답변"));

        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.commentContent()).isEqualTo("답변");
        assertThat(board.getIsAnswered()).isTrue();
    }

    @Test
    void deletingAnotherReplyKeepsAnsweredWhenDeletedStaffReplyRemains() {
        UserContextHolder.setUserId(42L);
        UserContextHolder.setUserRole(Role.MANAGER);
        QnaBoard board = board();
        QnaComment deletedStaffReply = QnaComment.createByStaff(board, 20L, "이전 답변");
        ReflectionTestUtils.setField(deletedStaffReply, "id", 2L);
        ReflectionTestUtils.setField(deletedStaffReply, "userId", null);
        QnaComment otherReply = QnaComment.createByStaff(board, 42L, "새 답변");
        ReflectionTestUtils.setField(otherReply, "id", 3L);
        board.addComment(deletedStaffReply);
        board.addComment(otherReply);
        given(boards.findByIdWithComments(1L)).willReturn(Optional.of(board));
        given(comments.existsById(3L)).willReturn(true);

        service.deleteComment(1L, 3L);

        assertThat(board.getComments()).containsExactly(deletedStaffReply);
        assertThat(board.getIsAnswered()).isTrue();
    }

    @Test
    void legacyStudentCommentDoesNotCountAsStaffAnswer() {
        UserContextHolder.setUserId(42L);
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        QnaBoard board = board();
        QnaComment legacyStudentComment = QnaComment.createByStaff(board, 20L, "옛 학생 댓글");
        ReflectionTestUtils.setField(legacyStudentComment, "id", 2L);
        ReflectionTestUtils.setField(legacyStudentComment, "userId", null);
        ReflectionTestUtils.setField(legacyStudentComment, "studentId", 30L);
        board.addComment(legacyStudentComment);
        assertThat(board.getIsAnswered()).isFalse();

        QnaComment staffReply = QnaComment.createByStaff(board, 42L, "교사 답변");
        ReflectionTestUtils.setField(staffReply, "id", 3L);
        board.addComment(staffReply);
        assertThat(board.getIsAnswered()).isTrue();
        given(boards.findByIdWithComments(1L)).willReturn(Optional.of(board));
        given(comments.existsById(3L)).willReturn(true);

        service.deleteComment(1L, 3L);

        assertThat(board.getComments()).containsExactly(legacyStudentComment);
        assertThat(board.getIsAnswered()).isFalse();
    }

    private static QnaBoard board() {
        QnaBoard board = QnaBoard.create(10L, "질문", "내용");
        ReflectionTestUtils.setField(board, "id", 1L);
        return board;
    }
}
