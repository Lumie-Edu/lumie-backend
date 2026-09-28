package com.lumie.classroom.application.service;

import com.lumie.classroom.application.dto.ClassEnrollmentResponse;
import com.lumie.classroom.application.dto.ClassResponse;
import com.lumie.classroom.application.dto.CreateClassRequest;
import com.lumie.classroom.application.dto.EnrollStudentsRequest;
import com.lumie.classroom.application.dto.SchedulePatternRequest;
import com.lumie.classroom.application.dto.UpdateClassRequest;
import com.lumie.classroom.application.dto.UpdateEnrollmentRequest;
import com.lumie.classroom.api.ClassEnrollmentCreatedEvent;
import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.staff.api.StaffService;
import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.repository.ClassEnrollmentRepository;
import com.lumie.classroom.domain.repository.ClassRepository;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import com.lumie.classroom.domain.vo.SchedulePattern;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.test.ReflectionTestUtils;
import com.lumie.common.test.TenantContextExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("ClassCommandService 테스트")
class ClassCommandServiceTest {

    @Mock private ClassRepository classRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private StaffService staffService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ClassCommandService commandService;

    @Nested
    @DisplayName("createClass")
    class CreateClass {

        @Test
        @DisplayName("정상 생성 시 ClassResponse를 반환한다")
        void createClass_success() {
            // given
            CreateClassRequest request = new CreateClassRequest(
                    "수학 기초반", 10L, null, "설명");

            given(staffService.getStaff(any(), eq(10L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "kim", "김선생", null, null, true, "ACTIVE", null, null)));
            given(classRepository.save(any(ClassEntity.class)))
                    .willAnswer(inv -> {
                        ClassEntity entity = inv.getArgument(0);
                        ReflectionTestUtils.setField(entity, "id", 1L);
                        return entity;
                    });

            // when
            ClassResponse response = commandService.createClass(request);

            // then
            assertThat(response.name()).isEqualTo("수학 기초반");
        }

        @Test
        @DisplayName("존재하지 않는 강사로 생성 시 BusinessException이 발생한다")
        void createClass_teacherNotFound_throwsException() {
            CreateClassRequest request = new CreateClassRequest(
                    "수학반", 999L, null, null);

            given(staffService.getStaff(any(), eq(999L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.createClass(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("요일별 시간이 다르면 같은 강사 수업을 생성할 수 있다")
        void createClass_perDaySchedule_allowsDifferentDayTime() throws Exception {
            CreateClassRequest request = new CreateClassRequest(
                    "수학 심화반",
                    10L,
                    new SchedulePatternRequest(
                            null,
                            null,
                            null,
                            List.of(
                                    new SchedulePatternRequest.ScheduleSessionRequest("WED", "15:00", "16:00"),
                                    new SchedulePatternRequest.ScheduleSessionRequest("FRI", "18:00", "19:00")
                            )
                    ),
                    null);
            ClassEntity existing = buildClassEntity(
                    2L,
                    new SchedulePattern(
                            null,
                            null,
                            null,
                            List.of(new SchedulePattern.ScheduleSession("WED", "16:00", "17:00"))
                    )
            );

            given(staffService.getStaff(any(), eq(10L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "kim", "김선생", null, null, true, "ACTIVE", null, null)));
            given(classRepository.findAllByTeacherId(10L)).willReturn(List.of(existing));
            given(classRepository.save(any(ClassEntity.class)))
                    .willAnswer(inv -> {
                        ClassEntity entity = inv.getArgument(0);
                        ReflectionTestUtils.setField(entity, "id", 1L);
                        return entity;
                    });

            ClassResponse response = commandService.createClass(request);

            assertThat(response.schedulePattern().sessions()).hasSize(2);
            assertThat(response.schedulePattern().sessions())
                    .extracting("day", "startTime", "endTime")
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("WED", "15:00", "16:00"),
                            org.assertj.core.groups.Tuple.tuple("FRI", "18:00", "19:00")
                    );
        }

        @Test
        @DisplayName("요일별 시간 중 같은 요일 시간이 겹치면 생성할 수 없다")
        void createClass_perDaySchedule_rejectsSameDayOverlap() throws Exception {
            CreateClassRequest request = new CreateClassRequest(
                    "수학 심화반",
                    10L,
                    new SchedulePatternRequest(
                            null,
                            null,
                            null,
                            List.of(new SchedulePatternRequest.ScheduleSessionRequest("WED", "15:30", "16:30"))
                    ),
                    null);
            ClassEntity existing = buildClassEntity(
                    2L,
                    new SchedulePattern(
                            null,
                            null,
                            null,
                            List.of(new SchedulePattern.ScheduleSession("WED", "15:00", "16:00"))
                    )
            );

            given(staffService.getStaff(any(), eq(10L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "kim", "김선생", null, null, true, "ACTIVE", null, null)));
            given(classRepository.findAllByTeacherId(10L)).willReturn(List.of(existing));

            assertThatThrownBy(() -> commandService.createClass(request))
                    .isInstanceOf(BusinessException.class);
        }

    }

    @Nested
    @DisplayName("SchedulePatternRequest")
    class SchedulePatternValidation {

        @Test
        @DisplayName("partial schedulePattern은 유효하지 않다")
        void partialSchedulePattern_isInvalid() {
            SchedulePatternRequest request = new SchedulePatternRequest(List.of("MON"), null, null, null);

            assertThat(request.isScheduleShapeValid()).isFalse();
        }

        @Test
        @DisplayName("empty sessions schedulePattern은 유효하지 않다")
        void emptySessionsSchedulePattern_isInvalid() {
            SchedulePatternRequest request = new SchedulePatternRequest(null, null, null, List.of());

            assertThat(request.isScheduleShapeValid()).isFalse();
        }

        @Test
        @DisplayName("null session을 포함한 schedulePattern은 유효하지 않다")
        void nullSessionSchedulePattern_isInvalid() {
            SchedulePatternRequest request = new SchedulePatternRequest(
                    null,
                    null,
                    null,
                    java.util.Arrays.asList((SchedulePatternRequest.ScheduleSessionRequest) null));

            assertThat(request.isScheduleShapeValid()).isFalse();
            assertThat(request.isScheduleTimeRangeValid()).isFalse();
        }

        @Test
        @DisplayName("요일별 session schedulePattern은 유효하다")
        void perDaySessionSchedulePattern_isValid() {
            SchedulePatternRequest request = new SchedulePatternRequest(
                    null,
                    null,
                    null,
                    List.of(new SchedulePatternRequest.ScheduleSessionRequest("MON", "15:00", "16:00")));

            assertThat(request.isScheduleShapeValid()).isTrue();
            assertThat(request.isScheduleTimeRangeValid()).isTrue();
        }

        @Test
        @DisplayName("역전된 시간 범위는 유효하지 않다")
        void reversedTimeRange_isInvalid() {
            SchedulePatternRequest request = new SchedulePatternRequest(
                    null,
                    null,
                    null,
                    List.of(new SchedulePatternRequest.ScheduleSessionRequest("MON", "16:00", "15:00")));

            assertThat(request.isScheduleTimeRangeValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateClass")
    class UpdateClass {

        @Test
        @DisplayName("정상 업데이트 시 변경된 ClassResponse를 반환한다")
        void updateClass_success() throws Exception {
            ClassEntity entity = buildClassEntity(1L);
            UpdateClassRequest request = new UpdateClassRequest(
                    "변경된 이름", null, null, null);

            given(classRepository.findById(1L)).willReturn(Optional.of(entity));
            given(classRepository.save(any(ClassEntity.class))).willAnswer(inv -> inv.getArgument(0));

            ClassResponse response = commandService.updateClass(1L, request);

            assertThat(response.name()).isEqualTo("변경된 이름");
        }

        @Test
        @DisplayName("존재하지 않는 수업 업데이트 시 ClassNotFoundException이 발생한다")
        void updateClass_notFound_throwsException() {
            given(classRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.updateClass(999L,
                    new UpdateClassRequest(null, null, null, null)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("deleteClass")
    class DeleteClass {

        @Test
        @DisplayName("삭제 시 하드 삭제(delete)가 호출된다")
        void deleteClass_hardDeletes() throws Exception {
            ClassEntity entity = buildClassEntity(1L);
            given(classRepository.findById(1L)).willReturn(Optional.of(entity));

            commandService.deleteClass(1L);

            verify(classRepository).delete(entity);
        }
    }

    @Nested
    @DisplayName("enrollStudents")
    class EnrollStudents {

        @Test
        @DisplayName("정상 등록 시 수강생 목록을 반환한다")
        void enrollStudents_success() throws Exception {
            ClassEntity classEntity = buildClassEntity(1L);
            EnrollStudentsRequest request = new EnrollStudentsRequest(List.of(100L, 200L));

            given(classRepository.findByIdWithLock(1L)).willReturn(Optional.of(classEntity));
            given(enrollmentRepository.existsByClassEntityIdAndStudentId(anyLong(), anyLong())).willReturn(false);
            given(enrollmentRepository.saveAll(any()))
                    .willAnswer(inv -> {
                        List<ClassEnrollment> enrollments = inv.getArgument(0);
                        for (int i = 0; i < enrollments.size(); i++) {
                            ReflectionTestUtils.setField(enrollments.get(i), "id", (long) (i + 1));
                        }
                        return enrollments;
                    });

            List<ClassEnrollmentResponse> responses = commandService.enrollStudents(1L, request);

            assertThat(responses).hasSize(2);
            verify(eventPublisher).publishEvent(new ClassEnrollmentCreatedEvent(
                    1L, "test-tenant", 1L, List.of(100L, 200L)));
        }

        @Test
        @DisplayName("중복 수강 시 DuplicateEnrollmentException이 발생한다")
        void enrollStudents_duplicate_throwsException() throws Exception {
            ClassEntity classEntity = buildClassEntity(1L);
            given(classRepository.findByIdWithLock(1L)).willReturn(Optional.of(classEntity));
            given(enrollmentRepository.existsByClassEntityIdAndStudentId(1L, 100L)).willReturn(true);

            assertThatThrownBy(() -> commandService.enrollStudents(1L,
                    new EnrollStudentsRequest(List.of(100L))))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("removeEnrollment")
    class RemoveEnrollment {

        @Test
        @DisplayName("정상 제거 시 enrollment가 삭제된다")
        void removeEnrollment_success() throws Exception {
            ClassEntity classEntity = buildClassEntity(1L);
            ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 100L);
            ReflectionTestUtils.setField(enrollment, "id", 10L);

            given(enrollmentRepository.findById(10L)).willReturn(Optional.of(enrollment));

            commandService.removeEnrollment(1L, 10L);

            verify(enrollmentRepository).delete(enrollment);
        }

        @Test
        @DisplayName("다른 수업의 enrollment 제거 시 EnrollmentNotFoundException이 발생한다")
        void removeEnrollment_wrongClassId_throwsException() throws Exception {
            ClassEntity classEntity = buildClassEntity(1L);
            ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 100L);
            ReflectionTestUtils.setField(enrollment, "id", 10L);

            given(enrollmentRepository.findById(10L)).willReturn(Optional.of(enrollment));

            assertThatThrownBy(() -> commandService.removeEnrollment(999L, 10L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateEnrollmentStatus")
    class UpdateEnrollmentStatus {

        @Test
        @DisplayName("DROPPED로 상태 변경 시 성공한다")
        void updateEnrollmentStatus_toDrop_success() throws Exception {
            ClassEntity classEntity = buildClassEntity(1L);
            ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 100L);
            ReflectionTestUtils.setField(enrollment, "id", 10L);

            given(enrollmentRepository.findById(10L)).willReturn(Optional.of(enrollment));
            given(enrollmentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ClassEnrollmentResponse response = commandService.updateEnrollmentStatus(
                    1L, 10L, new UpdateEnrollmentRequest("DROPPED"));

            assertThat(response.status()).isEqualTo(EnrollmentStatus.DROPPED);
        }

        @Test
        @DisplayName("잘못된 상태값으로 변경 시 BusinessException이 발생한다")
        void updateEnrollmentStatus_invalidStatus_throwsException() throws Exception {
            ClassEntity classEntity = buildClassEntity(1L);
            ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 100L);
            ReflectionTestUtils.setField(enrollment, "id", 10L);

            given(enrollmentRepository.findById(10L)).willReturn(Optional.of(enrollment));

            assertThatThrownBy(() -> commandService.updateEnrollmentStatus(
                    1L, 10L, new UpdateEnrollmentRequest("INVALID")))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    @DisplayName("dropActiveEnrollmentsForStudent는 활성 수강을 DROPPED로 변경한다")
    void dropActiveEnrollmentsForStudent_dropsActiveEnrollments() throws Exception {
        ClassEntity classEntity = buildClassEntity(1L);
        ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 100L);
        ReflectionTestUtils.setField(enrollment, "id", 10L);
        List<ClassEnrollment> enrollments = List.of(enrollment);

        given(enrollmentRepository.findAllByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED))
                .willReturn(enrollments);

        commandService.dropActiveEnrollmentsForStudent(100L);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(enrollment.getDroppedAt()).isNotNull();
        verify(enrollmentRepository).saveAll(enrollments);
    }

    // -- helpers --

    private ClassEntity buildClassEntity(Long id) throws Exception {
        ClassEntity entity = ClassEntity.create(
                "수학 기초반", 10L, null, "설명");
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private ClassEntity buildClassEntity(Long id, SchedulePattern schedulePattern) throws Exception {
        ClassEntity entity = ClassEntity.create(
                "수학 기초반", 10L, schedulePattern, "설명");
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

}
