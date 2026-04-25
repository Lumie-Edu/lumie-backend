package com.lumie.cls.application.service;

import com.lumie.cls.application.dto.ClassEnrollmentResponse;
import com.lumie.cls.application.dto.ClassResponse;
import com.lumie.cls.application.dto.CreateClassRequest;
import com.lumie.cls.application.dto.EnrollStudentsRequest;
import com.lumie.cls.application.dto.UpdateClassRequest;
import com.lumie.cls.application.dto.UpdateEnrollmentRequest;
import com.lumie.cls.application.port.out.AcademyServicePort;
import com.lumie.cls.application.port.out.AdminServicePort;
import com.lumie.cls.domain.entity.ClassEnrollment;
import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.exception.ClassNotFoundException;
import com.lumie.cls.domain.exception.DuplicateEnrollmentException;
import com.lumie.cls.domain.exception.EnrollmentNotFoundException;
import com.lumie.cls.domain.repository.ClassEnrollmentRepository;
import com.lumie.cls.domain.repository.ClassRepository;
import com.lumie.cls.domain.vo.EnrollmentStatus;
import com.lumie.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassCommandService 테스트")
class ClassCommandServiceTest {

    @Mock private ClassRepository classRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private AcademyServicePort academyServicePort;
    @Mock private AdminServicePort adminServicePort;
    @InjectMocks private ClassCommandService commandService;

    @Nested
    @DisplayName("createClass")
    class CreateClass {

        @Test
        @DisplayName("정상 생성 시 ClassResponse를 반환한다")
        void createClass_success() {
            // given
            CreateClassRequest request = new CreateClassRequest(
                    "수학 기초반", 1L, 10L, null, "설명");

            given(academyServicePort.getAcademy(1L))
                    .willReturn(Optional.of(new AcademyServicePort.AcademyInfo(1L, "테스트학원", true)));
            given(adminServicePort.getAdmin(10L))
                    .willReturn(Optional.of(new AdminServicePort.AdminInfo(10L, "김선생")));
            given(classRepository.existsByNameAndAcademyId("수학 기초반", 1L)).willReturn(false);
            given(classRepository.save(any(ClassEntity.class)))
                    .willAnswer(inv -> {
                        ClassEntity entity = inv.getArgument(0);
                        setField(entity, "id", 1L);
                        return entity;
                    });

            // when
            ClassResponse response = commandService.createClass(request);

            // then
            assertThat(response.name()).isEqualTo("수학 기초반");
        }

        @Test
        @DisplayName("존재하지 않는 학원으로 생성 시 BusinessException이 발생한다")
        void createClass_academyNotFound_throwsException() {
            CreateClassRequest request = new CreateClassRequest(
                    "수학반", 999L, null, null, null);

            given(academyServicePort.getAcademy(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.createClass(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("비활성 학원으로 생성 시 BusinessException이 발생한다")
        void createClass_inactiveAcademy_throwsException() {
            CreateClassRequest request = new CreateClassRequest(
                    "수학반", 1L, null, null, null);

            given(academyServicePort.getAcademy(1L))
                    .willReturn(Optional.of(new AcademyServicePort.AcademyInfo(1L, "학원", false)));

            assertThatThrownBy(() -> commandService.createClass(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 강사로 생성 시 BusinessException이 발생한다")
        void createClass_teacherNotFound_throwsException() {
            CreateClassRequest request = new CreateClassRequest(
                    "수학반", 1L, 999L, null, null);

            given(academyServicePort.getAcademy(1L))
                    .willReturn(Optional.of(new AcademyServicePort.AcademyInfo(1L, "학원", true)));
            given(adminServicePort.getAdmin(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.createClass(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("중복 수업명으로 생성 시 BusinessException이 발생한다")
        void createClass_duplicateName_throwsException() {
            CreateClassRequest request = new CreateClassRequest(
                    "수학반", 1L, null, null, null);

            given(academyServicePort.getAcademy(1L))
                    .willReturn(Optional.of(new AcademyServicePort.AcademyInfo(1L, "학원", true)));
            given(classRepository.existsByNameAndAcademyId("수학반", 1L)).willReturn(true);

            assertThatThrownBy(() -> commandService.createClass(request))
                    .isInstanceOf(BusinessException.class);
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
                    .isInstanceOf(ClassNotFoundException.class);
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
                            setField(enrollments.get(i), "id", (long) (i + 1));
                        }
                        return enrollments;
                    });

            List<ClassEnrollmentResponse> responses = commandService.enrollStudents(1L, request);

            assertThat(responses).hasSize(2);
        }

        @Test
        @DisplayName("중복 수강 시 DuplicateEnrollmentException이 발생한다")
        void enrollStudents_duplicate_throwsException() throws Exception {
            ClassEntity classEntity = buildClassEntity(1L);
            given(classRepository.findByIdWithLock(1L)).willReturn(Optional.of(classEntity));
            given(enrollmentRepository.existsByClassEntityIdAndStudentId(1L, 100L)).willReturn(true);

            assertThatThrownBy(() -> commandService.enrollStudents(1L,
                    new EnrollStudentsRequest(List.of(100L))))
                    .isInstanceOf(DuplicateEnrollmentException.class);
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
            setField(enrollment, "id", 10L);

            given(enrollmentRepository.findById(10L)).willReturn(Optional.of(enrollment));

            commandService.removeEnrollment(1L, 10L);

            verify(enrollmentRepository).delete(enrollment);
        }

        @Test
        @DisplayName("다른 수업의 enrollment 제거 시 EnrollmentNotFoundException이 발생한다")
        void removeEnrollment_wrongClassId_throwsException() throws Exception {
            ClassEntity classEntity = buildClassEntity(1L);
            ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 100L);
            setField(enrollment, "id", 10L);

            given(enrollmentRepository.findById(10L)).willReturn(Optional.of(enrollment));

            assertThatThrownBy(() -> commandService.removeEnrollment(999L, 10L))
                    .isInstanceOf(EnrollmentNotFoundException.class);
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
            setField(enrollment, "id", 10L);

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
            setField(enrollment, "id", 10L);

            given(enrollmentRepository.findById(10L)).willReturn(Optional.of(enrollment));

            assertThatThrownBy(() -> commandService.updateEnrollmentStatus(
                    1L, 10L, new UpdateEnrollmentRequest("INVALID")))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // -- helpers --

    private ClassEntity buildClassEntity(Long id) throws Exception {
        ClassEntity entity = ClassEntity.create(
                1L, "수학 기초반", 10L, null, "설명");
        setField(entity, "id", id);
        return entity;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}
