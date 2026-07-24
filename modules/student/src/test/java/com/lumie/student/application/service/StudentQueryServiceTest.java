package com.lumie.student.application.service;

import com.lumie.auth.api.AuthService;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.domain.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentQueryServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private AuthService authService;
    @Mock private ClassService classService;
    @InjectMocks private StudentQueryService studentQueryService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void searchStudents_filtersByActiveEnrollmentInClass() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Long> enrolledStudentIds = List.of(10L, 20L);
        given(classService.getEnrolledStudentIds("test-tenant", 1L, 7L)).willReturn(enrolledStudentIds);
        given(studentRepository.search(true, null, enrolledStudentIds, null, pageable))
                .willReturn(Page.empty(pageable));

        studentQueryService.searchStudents(true, null, 7L, null, pageable);

        verify(studentRepository).search(true, null, enrolledStudentIds, null, pageable);
    }

    @Test
    void searchStudents_excludesStudentsWithActiveEnrollmentForUnassignedFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Long> activeEnrollmentStudentIds = List.of(10L, 20L);
        given(classService.getActiveEnrolledStudentIds("test-tenant", 1L)).willReturn(activeEnrollmentStudentIds);
        given(studentRepository.search(isNull(), isNull(), isNull(), eq(activeEnrollmentStudentIds), eq(pageable)))
                .willReturn(Page.empty(pageable));

        studentQueryService.searchStudents(null, null, null, false, pageable);

        verify(studentRepository).search(null, null, null, activeEnrollmentStudentIds, pageable);
    }

    @Test
    void searchStudents_filtersStudentsWithAnyActiveEnrollment() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Long> activeEnrollmentStudentIds = List.of(10L, 20L);
        given(classService.getActiveEnrolledStudentIds("test-tenant", 1L)).willReturn(activeEnrollmentStudentIds);
        given(studentRepository.search(isNull(), isNull(), eq(activeEnrollmentStudentIds), isNull(), eq(pageable)))
                .willReturn(Page.empty(pageable));

        studentQueryService.searchStudents(null, null, null, true, pageable);

        verify(studentRepository).search(null, null, activeEnrollmentStudentIds, null, pageable);
    }

    @Test
    void searchStudents_prioritizesClassFilterOverEnrollmentState() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Long> enrolledStudentIds = List.of(10L);
        given(classService.getEnrolledStudentIds("test-tenant", 1L, 7L)).willReturn(enrolledStudentIds);
        given(studentRepository.search(isNull(), isNull(), eq(enrolledStudentIds), isNull(), eq(pageable)))
                .willReturn(Page.empty(pageable));

        studentQueryService.searchStudents(null, null, 7L, false, pageable);

        verify(studentRepository).search(null, null, enrolledStudentIds, null, pageable);
        verify(classService, never()).getActiveEnrolledStudentIds("test-tenant", 1L);
    }
}
