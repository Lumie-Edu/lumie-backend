package com.lumie.content.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.QnaComment;
import com.lumie.content.domain.repository.QnaBoardRepository;
import com.lumie.staff.api.StaffService;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("QnaQueryService 테스트")
class QnaQueryServiceTest {

    @Mock private QnaBoardRepository qnaBoardRepository;
    @Mock private StudentService studentService;
    @Mock private StaffService staffService;
    @InjectMocks private QnaQueryService queryService;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenant("test-tenant");
    }

    @AfterEach
    void tearDownTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("목록에서 작성자 학생을 찾을 수 없으면 '삭제된 학생'으로 반환한다")
    void listQnas_deletedStudentName() {
        QnaBoard qna = qna(1L, 100L);
        given(qnaBoardRepository.search(null, null, null, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(qna), PageRequest.of(0, 20), 1));
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(emptyStudents());

        var response = queryService.listQnas(null, null, PageRequest.of(0, 20));

        assertThat(response.getContent().get(0).qnaUserName()).isEqualTo("삭제된 학생");
    }

    @Test
    @DisplayName("상세에서 삭제된 학생 작성자와 삭제된 선생님 댓글 작성자를 구분한다")
    void getQna_deletedStudentAndStaffNames() {
        QnaBoard qna = qna(1L, 100L);
        qna.addComment(QnaComment.createByStaff(qna, 900L, "답변"));
        given(qnaBoardRepository.findByIdWithComments(1L)).willReturn(Optional.of(qna));
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(emptyStudents());
        given(staffService.getStaffByUserId("test-tenant", 900L)).willReturn(Optional.empty());

        var response = queryService.getQna(1L);

        assertThat(response.qnaUserName()).isEqualTo("삭제된 학생");
        assertThat(response.comments().get(0).staffName()).isEqualTo("삭제된 선생님");
    }

    @Test
    void getQna_deletedStaffReferenceKeepsReplyAndTeacherPlaceholder() {
        QnaBoard qna = qna(1L, 100L);
        QnaComment reply = QnaComment.createByStaff(qna, 900L, "답변 본문");
        ReflectionTestUtils.setField(reply, "userId", null);
        qna.addComment(reply);
        given(qnaBoardRepository.findByIdWithComments(1L)).willReturn(Optional.of(qna));
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(emptyStudents());

        var response = queryService.getQna(1L);

        assertThat(response.comments().get(0).staffName()).isEqualTo("삭제된 선생님");
        assertThat(response.comments().get(0).commentContent()).isEqualTo("답변 본문");
        verifyNoInteractions(staffService);
    }

    @Test
    void getQna_legacyStudentCommentIsNotNamedAsDeletedTeacher() {
        QnaBoard qna = qna(1L, 100L);
        QnaComment comment = QnaComment.createByStaff(qna, 900L, "옛 학생 댓글");
        ReflectionTestUtils.setField(comment, "userId", null);
        ReflectionTestUtils.setField(comment, "studentId", 30L);
        qna.addComment(comment);
        given(qnaBoardRepository.findByIdWithComments(1L)).willReturn(Optional.of(qna));
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(emptyStudents());

        var response = queryService.getQna(1L);

        assertThat(response.comments().get(0).studentId()).isEqualTo(30L);
        assertThat(response.comments().get(0).staffName()).isNull();
        assertThat(response.comments().get(0).commentContent()).isEqualTo("옛 학생 댓글");
        verifyNoInteractions(staffService);
    }

    @Test
    void getQna_nullAuthorIdKeepsContentAndDeletedStudentPlaceholder() {
        QnaBoard qna = qna(1L, 100L);
        ReflectionTestUtils.setField(qna, "qnaUserId", null);
        given(qnaBoardRepository.findByIdWithComments(1L)).willReturn(Optional.of(qna));

        var response = queryService.getQna(1L);

        assertThat(response.qnaUserId()).isNull();
        assertThat(response.qnaUserName()).isEqualTo("삭제된 학생");
        assertThat(response.qnaContent()).isEqualTo("내용");
        verifyNoInteractions(studentService, staffService);
    }

    @Test
    void listQnas_allDeletedAuthorsStillHavePlaceholder() {
        QnaBoard qna = qna(1L, 100L);
        ReflectionTestUtils.setField(qna, "qnaUserId", null);
        var pageable = PageRequest.of(0, 20);
        given(qnaBoardRepository.search(null, null, null, pageable))
                .willReturn(new PageImpl<>(List.of(qna), pageable, 1));

        var response = queryService.listQnas(null, null, pageable);

        assertThat(response.getContent().get(0).qnaUserName()).isEqualTo("삭제된 학생");
        verifyNoInteractions(studentService, staffService);
    }

    @Test
    void studentCannotViewQnaWithDeletedAuthor() {
        QnaBoard qna = qna(1L, 100L);
        ReflectionTestUtils.setField(qna, "qnaUserId", null);
        given(qnaBoardRepository.findById(1L)).willReturn(Optional.of(qna));

        assertThat(queryService.canUserViewQna(1L, "STUDENT", 100L)).isFalse();
    }

    @Test
    void qnaAccessWithoutRoleIsDenied() {
        assertThat(queryService.canUserViewQna(1L, null, 100L)).isFalse();
    }

    private static QnaBoard qna(Long id, Long userId) {
        QnaBoard qna = QnaBoard.create(userId, "질문", "내용");
        ReflectionTestUtils.setField(qna, "id", id);
        return qna;
    }

    private static StudentService.StudentListData emptyStudents() {
        return new StudentService.StudentListData(List.of(), 0, 0, 0);
    }
}
