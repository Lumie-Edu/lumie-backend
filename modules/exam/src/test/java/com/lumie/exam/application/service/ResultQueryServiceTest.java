package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.OmrGradingJob;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

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

    @Test
    @DisplayName("OMR job 상세는 현재 공식화된 submission 수를 savedCount로 노출한다")
    void getOmrJobStatus_usesDerivedOfficializedCount() {
        OmrGradingJob job = job(31L, 9L);
        given(jobRepository.findById(31L)).willReturn(java.util.Optional.of(job));
        given(resultRepository.countOfficializedByJobId(31L)).willReturn(4);

        var response = resultQueryService.getOmrJobStatus(9L, 31L);

        assertThat(response.savedCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("OMR job 목록은 저장 컬럼 대신 공식화된 수를 batch로 계산한다")
    void getOmrJobs_usesDerivedOfficializedCounts() {
        OmrGradingJob first = job(41L, 9L);
        OmrGradingJob second = job(42L, 9L);
        ExamResultRepository.JobSavedCountRow row = mock(ExamResultRepository.JobSavedCountRow.class);
        given(row.getJobId()).willReturn(41L);
        given(row.getSavedCount()).willReturn(2L);
        given(jobRepository.findByExamIdOrderByCreatedAtDesc(9L)).willReturn(List.of(first, second));
        given(resultRepository.countOfficializedByJobIds(List.of(41L, 42L))).willReturn(List.of(row));

        var responses = resultQueryService.getOmrJobs(9L);

        assertThat(responses).extracting("savedCount").containsExactly(2, 0);
    }

    private OmrGradingJob job(Long jobId, Long examId) {
        OmrGradingJob job = mock(OmrGradingJob.class);
        Exam exam = mock(Exam.class);
        given(exam.getId()).willReturn(examId);
        given(job.getId()).willReturn(jobId);
        given(job.getExam()).willReturn(exam);
        given(job.getStatus()).willReturn(com.lumie.exam.domain.vo.JobStatus.COMPLETED);
        given(job.getTotalImages()).willReturn(5);
        given(job.getProcessedImages()).willReturn(5);
        given(job.getSuccessCount()).willReturn(4);
        given(job.getFailCount()).willReturn(1);
        given(job.getCreatedAt()).willReturn(java.time.Instant.parse("2026-07-25T00:00:00Z"));
        return job;
    }
}
