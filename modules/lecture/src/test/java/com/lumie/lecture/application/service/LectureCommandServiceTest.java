package com.lumie.lecture.application.service;

import com.lumie.classroom.api.ClassService;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.test.ReflectionTestUtils;
import com.lumie.common.test.TenantContextExtension;
import com.lumie.lecture.application.dto.request.CreateLectureRequest;
import com.lumie.lecture.application.dto.request.UpdateLectureRequest;
import com.lumie.lecture.application.dto.response.LectureResponse;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.entity.LectureClassTarget;
import com.lumie.lecture.domain.repository.LectureClassTargetRepository;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureVisibility;
import com.lumie.staff.api.StaffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("LectureCommandService 테스트")
class LectureCommandServiceTest {

    @Mock private LectureRepository lectureRepository;
    @Mock private LectureClassTargetRepository targetRepository;
    @Mock private StaffService staffService;
    @Mock private ClassService classService;
    @InjectMocks private LectureCommandService commandService;

    @BeforeEach
    void setUpUser() {
        UserContextHolder.setUserId(100L);
        UserContextHolder.setUserRole(Role.OWNER);
    }

    @AfterEach
    void tearDownUser() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("학생 공개 강의는 대상 수업이 없으면 생성할 수 없다")
    void create_studentVisibilityWithoutTargets_throws() {
        CreateLectureRequest request = new CreateLectureRequest(
                null,
                List.of(),
                10L,
                "수업 영상",
                null,
                "https://www.youtube.com/watch?v=abc123",
                "STUDENT",
                null
        );

        assertThatThrownBy(() -> commandService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("강의 생성 시 classId와 classIds를 중복 제거하고 대상 수업으로 저장한다")
    void create_deduplicatesAndStoresClassTargets() throws Exception {
        CreateLectureRequest request = new CreateLectureRequest(
                2L,
                List.of(2L, 3L),
                10L,
                "수업 영상",
                null,
                "https://www.youtube.com/watch?v=abc123",
                "STUDENT",
                null
        );
        given(classService.getClass("test-tenant", 2L)).willReturn(Optional.of(classData(2L)));
        given(classService.getClass("test-tenant", 3L)).willReturn(Optional.of(classData(3L)));
        given(lectureRepository.save(any(Lecture.class))).willAnswer(invocation -> {
            Lecture lecture = invocation.getArgument(0);
            ReflectionTestUtils.setField(lecture, "id", 1L);
            return lecture;
        });
        given(targetRepository.findClassIdsByLectureIdOrderByClassIdAsc(1L))
                .willReturn(List.of(2L, 3L));

        LectureResponse response = commandService.create(request);

        assertThat(response.classId()).isEqualTo(2L);
        assertThat(response.classIds()).containsExactly(2L, 3L);
        verify(targetRepository).deleteByLectureId(1L);
        ArgumentCaptor<LectureClassTarget> targetCaptor =
                ArgumentCaptor.forClass(LectureClassTarget.class);
        verify(targetRepository, org.mockito.Mockito.times(2)).save(targetCaptor.capture());
        assertThat(targetCaptor.getAllValues())
                .extracting(LectureClassTarget::getClassId)
                .containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("강사는 본인이 담당하지 않는 수업으로 강의를 공개할 수 없다")
    void create_instructorCannotTargetOtherTeacherClass() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        CreateLectureRequest request = new CreateLectureRequest(
                null,
                List.of(2L),
                10L,
                "수업 영상",
                null,
                "https://www.youtube.com/watch?v=abc123",
                "STUDENT",
                null
        );
        given(staffService.getStaffByUserId("test-tenant", 100L))
                .willReturn(Optional.of(staffData(10L)));
        given(classService.getClass("test-tenant", 2L))
                .willReturn(Optional.of(classData(2L, 11L)));

        assertThatThrownBy(() -> commandService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("대상 수업을 변경하지 않는 수정은 기존 class target 존재 여부를 재검증하지 않는다")
    void update_withoutTargetChange_doesNotRevalidateExistingTargets() throws Exception {
        Lecture lecture = Lecture.create(10L, "기존 제목", null,
                "https://www.youtube.com/watch?v=abc123", null, null);
        ReflectionTestUtils.setField(lecture, "id", 1L);
        UpdateLectureRequest request = new UpdateLectureRequest(
                null,
                null,
                "변경 제목",
                null,
                null,
                null,
                null
        );
        given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));
        given(targetRepository.findClassIdsByLectureIdOrderByClassIdAsc(1L)).willReturn(List.of());
        given(lectureRepository.save(lecture)).willReturn(lecture);

        LectureResponse response = commandService.update(1L, request);

        assertThat(response.title()).isEqualTo("변경 제목");
        assertThat(response.classIds()).isEmpty();
        verifyNoInteractions(classService);
    }

    @Test
    void targetlessStudentLectureRemainsEditableByStaffWithoutBecomingVisible() throws Exception {
        Lecture lecture = Lecture.create(10L, "기존 제목", null,
                "https://www.youtube.com/watch?v=abc123", LectureVisibility.STUDENT, null);
        ReflectionTestUtils.setField(lecture, "id", 1L);
        UpdateLectureRequest request = new UpdateLectureRequest(
                null, null, "변경 제목", null, null, null, null);
        given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));
        given(targetRepository.findClassIdsByLectureIdOrderByClassIdAsc(1L)).willReturn(List.of());
        given(lectureRepository.save(lecture)).willReturn(lecture);

        LectureResponse response = commandService.update(1L, request);

        assertThat(response.title()).isEqualTo("변경 제목");
        assertThat(response.classId()).isNull();
        assertThat(response.classIds()).isEmpty();
    }

    @Test
    @DisplayName("동일한 대상 수업으로 수정하면 target을 삭제하거나 재생성하지 않는다")
    void update_sameTargetClasses_doesNotResyncTargets() throws Exception {
        Lecture lecture = Lecture.create(10L, "기존 제목", null,
                "https://www.youtube.com/watch?v=abc123", null, null);
        ReflectionTestUtils.setField(lecture, "id", 1L);
        UpdateLectureRequest request = new UpdateLectureRequest(
                2L,
                List.of(2L),
                "기존 제목",
                null,
                "https://www.youtube.com/watch?v=abc123",
                "STUDENT",
                null
        );
        given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));
        given(targetRepository.findClassIdsByLectureIdOrderByClassIdAsc(1L)).willReturn(List.of(2L));
        given(lectureRepository.save(lecture)).willReturn(lecture);

        LectureResponse response = commandService.update(1L, request);

        assertThat(response.classIds()).containsExactly(2L);
        verifyNoInteractions(classService);
        verify(targetRepository, never()).deleteByLectureId(1L);
        verify(targetRepository, never()).save(any(LectureClassTarget.class));
    }

    private static ClassService.ClassData classData(Long classId) {
        return classData(classId, 10L);
    }

    private static ClassService.ClassData classData(Long classId, Long teacherId) {
        return new ClassService.ClassData(
                classId,
                "수학반 " + classId,
                1L,
                teacherId,
                List.of(),
                null,
                null,
                null,
                null
        );
    }

    private static StaffService.StaffData staffData(Long staffId) {
        return new StaffService.StaffData(
                staffId,
                100L,
                "teacher-100",
                "강사",
                "010-0000-0000",
                null,
                true,
                "ACTIVE",
                null,
                null
        );
    }

}
