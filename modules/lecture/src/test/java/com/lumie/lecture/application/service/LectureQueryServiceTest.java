package com.lumie.lecture.application.service;

import com.lumie.classroom.api.ClassService;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.test.ReflectionTestUtils;
import com.lumie.common.test.TenantContextExtension;
import com.lumie.lecture.application.dto.response.WatchResponse;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.entity.LectureClassTarget;
import com.lumie.lecture.domain.exception.LectureErrorCode;
import com.lumie.lecture.domain.repository.LectureClassTargetRepository;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureVisibility;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("LectureQueryService 테스트")
class LectureQueryServiceTest {

    @Mock private LectureRepository lectureRepository;
    @Mock private LectureClassTargetRepository targetRepository;
    @Mock private StudentService studentService;
    @Mock private ClassService classService;
    @InjectMocks private LectureQueryService queryService;

    @BeforeEach
    void setUpUser() {
        UserContextHolder.setUserId(100L);
    }

    @AfterEach
    void tearDownUser() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("수강 중인 학생은 대상 수업으로 공개된 강의를 시청할 수 있다")
    void getWatchUrls_enrolledTargetClass_success() throws Exception {
        Lecture lecture = studentLecture(1L);
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(studentList(50L));
        given(classService.isStudentEnrolledInClass("test-tenant", 50L, 2L))
                .willReturn(true);
        given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));
        given(targetRepository.findByLectureIdOrderByClassIdAsc(1L))
                .willReturn(List.of(target(lecture, 2L), target(lecture, 3L)));

        WatchResponse response = queryService.getWatchUrls(1L, 2L);

        assertThat(response.lectureId()).isEqualTo(1L);
        assertThat(response.visibility()).isEqualTo(LectureVisibility.STUDENT);
    }

    @Test
    @DisplayName("수강 중이어도 대상 수업이 아닌 classId로는 강의를 시청할 수 없다")
    void getWatchUrls_nonTargetClass_throws() throws Exception {
        Lecture lecture = studentLecture(1L);
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(studentList(50L));
        given(classService.isStudentEnrolledInClass("test-tenant", 50L, 4L))
                .willReturn(true);
        given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));
        given(targetRepository.findByLectureIdOrderByClassIdAsc(1L))
                .willReturn(List.of(target(lecture, 2L), target(lecture, 3L)));

        assertThatThrownBy(() -> queryService.getWatchUrls(1L, 4L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(LectureErrorCode.LECTURE_NOT_VISIBLE_TO_CLASS));
    }

    @Test
    @DisplayName("학생 역할에 연결된 Student 행이 없으면 404를 반환한다")
    void findByClassIdForStudent_withoutStudent_throwsNotFound() {
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(new StudentService.StudentListData(List.of(), 0, 0, 0));

        assertThatThrownBy(() -> queryService.findByClassIdForStudent(2L, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    @DisplayName("classId 없이 접근하면 학생이 수강 중인 대상 수업으로 시청할 수 있다")
    void getWatchUrls_withoutClassId_resolvesEnrolledTargetClass() throws Exception {
        Lecture lecture = studentLecture(1L);
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(studentList(50L));
        given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));
        given(targetRepository.findByLectureIdOrderByClassIdAsc(1L))
                .willReturn(List.of(target(lecture, 2L), target(lecture, 3L)));
        given(classService.isStudentEnrolledInClass("test-tenant", 50L, 2L))
                .willReturn(true);

        WatchResponse response = queryService.getWatchUrls(1L, null);

        assertThat(response.lectureId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("학생 수업별 목록은 요청한 수업 ID만 응답한다")
    void findByClassIdForStudent_returnsOnlyRequestedClassId() throws Exception {
        Lecture lecture = studentLecture(1L);
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(studentList(50L));
        given(classService.isStudentEnrolledInClass("test-tenant", 50L, 2L))
                .willReturn(true);
        given(lectureRepository.findVisibleByTargetClassId(2L, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(lecture), PageRequest.of(0, 20), 1));

        var response = queryService.findByClassIdForStudent(2L, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).classIds()).containsExactly(2L);
    }

    private static StudentService.StudentListData studentList(Long studentId) {
        StudentService.StudentData student = new StudentService.StudentData(
                studentId,
                100L,
                "학생",
                "010-0000-0000",
                true,
                null,
                null
        );
        return new StudentService.StudentListData(List.of(student), 1, 0, 1);
    }

    private static Lecture studentLecture(Long lectureId) throws Exception {
        Lecture lecture = Lecture.create(2L, 10L, "수업 영상", null,
                "https://www.youtube.com/watch?v=abc123", LectureVisibility.STUDENT, null);
        ReflectionTestUtils.setField(lecture, "id", lectureId);
        return lecture;
    }

    private static LectureClassTarget target(Lecture lecture, Long classId) {
        return LectureClassTarget.create(lecture, classId);
    }
}
