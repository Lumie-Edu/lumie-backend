package com.lumie.classroom.application.service;

import com.lumie.classroom.application.dto.ClassEnrollmentResponse;
import com.lumie.classroom.application.dto.ClassResponse;
import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.exception.ClassNotFoundException;
import com.lumie.classroom.domain.repository.ClassEnrollmentRepository;
import com.lumie.staff.api.StaffService;
import com.lumie.classroom.domain.repository.ClassRepository;
import com.lumie.classroom.domain.vo.ClassSearchCriteria;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import com.lumie.common.test.ReflectionTestUtils;
import com.lumie.common.test.TenantContextExtension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("ClassQueryService 테스트")
class ClassQueryServiceTest {

    @Mock private ClassRepository classRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private StaffService staffService;
    @InjectMocks private ClassQueryService queryService;

    @Test
    @DisplayName("getClass 성공 시 ClassResponse를 반환한다")
    void getClass_success() throws Exception {
        ClassEntity entity = buildClassEntity(1L);
        given(classRepository.findById(1L)).willReturn(Optional.of(entity));

        ClassResponse response = queryService.getClass(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("수학 기초반");
    }

    @Test
    @DisplayName("getClass 존재하지 않으면 ClassNotFoundException이 발생한다")
    void getClass_notFound_throwsException() {
        given(classRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getClass(999L))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    @DisplayName("listClasses 성공 시 페이지네이션된 결과를 반환한다")
    void listClasses_success() throws Exception {
        ClassEntity entity = buildClassEntity(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

        given(classRepository.findAll(any(ClassSearchCriteria.class), eq(pageable))).willReturn(page);

        Page<ClassResponse> response = queryService.listClasses(null, null, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("listClasses 필터 조건을 ClassSearchCriteria로 전달한다")
    void listClasses_withFilters() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(classRepository.findAll(any(ClassSearchCriteria.class), eq(pageable))).willReturn(emptyPage);

        Page<ClassResponse> response = queryService.listClasses(10L, "수학", pageable);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getClassesByTeacher 성공 시 해당 강사의 수업 목록을 반환한다")
    void getClassesByTeacher_success() throws Exception {
        ClassEntity entity = buildClassEntity(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

        given(classRepository.findByTeacherId(10L, pageable)).willReturn(page);

        Page<ClassResponse> response = queryService.getClassesByTeacher(10L, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getClassesByStudent 성공 시 해당 학생의 수업 목록을 반환한다")
    void getClassesByStudent_success() throws Exception {
        ClassEntity classEntity = buildClassEntity(1L);
        ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 42L);
        ReflectionTestUtils.setField(enrollment, "id", 10L);

        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassEnrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);

        given(enrollmentRepository.findByStudentIdAndStatus(42L, EnrollmentStatus.ENROLLED, pageable)).willReturn(page);

        Page<ClassResponse> response = queryService.getClassesByStudent(42L, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getEnrollments 성공 시 수강생 목록을 반환한다")
    void getEnrollments_success() throws Exception {
        ClassEntity classEntity = buildClassEntity(1L);
        ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 42L);
        ReflectionTestUtils.setField(enrollment, "id", 10L);

        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassEnrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);

        given(enrollmentRepository.findByClassEntityId(1L, pageable)).willReturn(page);

        Page<ClassEnrollmentResponse> response = queryService.getEnrollments(1L, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).studentId()).isEqualTo(42L);
    }

    // -- helpers --

    private ClassEntity buildClassEntity(Long id) throws Exception {
        ClassEntity entity = ClassEntity.create(
                "수학 기초반", 10L, null, "설명");
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
