package com.lumie.student.application.service;

import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.api.ContentService;
import com.lumie.student.domain.entity.Student;
import com.lumie.student.domain.exception.StudentErrorCode;
import com.lumie.student.domain.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentCommandService 테스트")
class StudentCommandServiceTest {

    private static final String TENANT = "inst-test";

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AuthService authService;

    @Mock
    private BillingService billingService;

    @Mock
    private ClassService classService;

    @Mock
    private ContentService contentService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StudentCommandService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("deleteStudent는 현재 수강 중인 학생 삭제를 막는다")
    void deleteStudent_activeEnrollment_blocksDeletion() {
        Student student = inactiveStudent(42L, 100L);
        given(studentRepository.findById(42L)).willReturn(Optional.of(student));
        given(classService.hasActiveEnrollment(TENANT, 42L)).willReturn(true);

        assertThatThrownBy(() -> service.deleteStudent(42L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.CANNOT_DELETE_STUDENT_WITH_ACTIVE_ENROLLMENT));

        verify(authService, never()).deleteUser(anyLong());
    }

    @Test
    @DisplayName("deleteStudent는 예정 상담 예약이 있는 학생 삭제를 막는다")
    void deleteStudent_upcomingReservation_blocksDeletion() {
        Student student = inactiveStudent(42L, 100L);
        given(studentRepository.findById(42L)).willReturn(Optional.of(student));
        given(classService.hasActiveEnrollment(TENANT, 42L)).willReturn(false);
        given(contentService.hasActiveOrUpcomingReservationsForStudent(TENANT, 42L)).willReturn(true);

        assertThatThrownBy(() -> service.deleteStudent(42L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.CANNOT_DELETE_STUDENT_WITH_UPCOMING_RESERVATION));

        verify(authService, never()).deleteUser(anyLong());
    }

    private static Student inactiveStudent(Long id, Long userId) {
        Student student = Student.create(userId, "student@example.com", "홍길동", "01012345678", null, null, null, null);
        student.deactivate();
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
