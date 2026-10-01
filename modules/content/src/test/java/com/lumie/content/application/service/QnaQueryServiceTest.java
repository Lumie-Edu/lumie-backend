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
    @DisplayName("상세에서 삭제된 학생 작성자와 삭제된 직원 댓글 작성자를 구분한다")
    void getQna_deletedStudentAndStaffNames() {
        QnaBoard qna = qna(1L, 100L);
        qna.addComment(QnaComment.createByAdmin(qna, 900L, "답변"));
        given(qnaBoardRepository.findByIdWithComments(1L)).willReturn(Optional.of(qna));
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(emptyStudents());
        given(staffService.getStaffByUserId("test-tenant", 900L)).willReturn(Optional.empty());

        var response = queryService.getQna(1L);

        assertThat(response.qnaUserName()).isEqualTo("삭제된 학생");
        assertThat(response.comments().get(0).adminName()).isEqualTo("삭제된 직원");
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
