package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResultQueryService 단위 테스트")
class ResultQueryServiceTest {

    @Mock
    private ExamResultRepository resultRepository;
    @Mock
    private OmrGradingJobRepository jobRepository;
    @Mock
    private OmrStoragePort storagePort;
    @Mock
    private StudentService studentService;

    @InjectMocks
    private ResultQueryService resultQueryService;

    @BeforeEach
    void setUpContext() {
        TenantContextHolder.setTenant("test-academy");
        UserContextHolder.setUserId(77L);
    }

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("현재 학생 시험 결과 조회는 userId를 studentId로 변환한 뒤 조회한다")
    void getCurrentStudentResults_usesStudentIdResolvedFromCurrentUser() {
        given(studentService.getStudentsByUserIds("test-academy", List.of(77L)))
                .willReturn(new StudentService.StudentListData(
                        List.of(new StudentService.StudentData(123L, 77L, "하지영", "01012345678", true, null, null)),
                        1,
                        0,
                        1
                ));
        given(resultRepository.findByStudentId(123L)).willReturn(List.of());

        resultQueryService.getCurrentStudentResults();

        verify(resultRepository).findByStudentId(123L);
    }

    @Test
    @DisplayName("현재 사용자에 연결된 학생이 없으면 STUDENT_NOT_FOUND를 반환한다")
    void getCurrentStudentResults_withoutStudent_throwsStudentNotFound() {
        given(studentService.getStudentsByUserIds("test-academy", List.of(77L)))
                .willReturn(new StudentService.StudentListData(List.of(), 0, 0, 0));

        assertThatThrownBy(() -> resultQueryService.getCurrentStudentResults())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.STUDENT_NOT_FOUND);
    }
}
