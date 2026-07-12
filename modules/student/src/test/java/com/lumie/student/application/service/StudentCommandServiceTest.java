package com.lumie.student.application.service;

import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.auth.Role;
import com.lumie.common.quota.MetricType;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.application.dto.request.StudentRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StudentCommandService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant(TENANT);
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("deleteStudent는 현재 수강 중인 학생을 자동 탈퇴시키고 삭제한다")
    void deleteStudent_activeEnrollment_dropsEnrollmentAndDeletes() {
        Student student = inactiveStudent(42L, 100L);
        given(studentRepository.findById(42L)).willReturn(Optional.of(student));
        given(authService.deleteUser(100L)).willReturn(new AuthService.DeleteUserResult(true, "ok"));

        service.deleteStudent(42L);

        verify(classService).dropActiveEnrollmentsForStudent(TENANT, 42L);
        verify(authService).deleteUser(100L);
    }

    @Test
    @DisplayName("registerStudent는 학부모 연락처 중복을 허용한다")
    void registerStudent_duplicateParentPhone_allowsRegistration() {
        StudentRequest request = new StudentRequest(
                "김민조",
                "01099999999",
                "양천고",
                2000,
                "01012345678",
                null
        );
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));
        given(studentRepository.existsByPhone("01099999999")).willReturn(false);
        given(authService.createUser("김민조", "01099999999", Role.STUDENT.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "01099999999"));
        given(studentRepository.save(any(Student.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.registerStudent(request);

        verify(studentRepository).existsByPhone("01099999999");
        verify(authService).createUser("김민조", "01099999999", Role.STUDENT.name(), 1L);
    }

    @Test
    @DisplayName("registerStudent는 빈 학부모 연락처를 null로 저장한다")
    void registerStudent_blankParentPhone_storesNull() {
        StudentRequest request = new StudentRequest(
                "김민조",
                "01099999999",
                "양천고",
                2000,
                "",
                null
        );
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));
        given(studentRepository.existsByPhone("01099999999")).willReturn(false);
        given(authService.createUser("김민조", "01099999999", Role.STUDENT.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "01099999999"));
        given(studentRepository.save(any(Student.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.registerStudent(request);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getParentPhone()).isNull();
    }

    @Test
    @DisplayName("registerStudent는 잘못된 학부모 연락처 형식을 막는다")
    void registerStudent_invalidParentPhone_blocksRegistration() {
        StudentRequest request = new StudentRequest(
                "김민조",
                "01099999999",
                "양천고",
                2000,
                "---",
                null
        );
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));
        given(studentRepository.existsByPhone("01099999999")).willReturn(false);

        assertThatThrownBy(() -> service.registerStudent(request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.INVALID_PHONE_NUMBER));

        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("updateStudent는 빈 학부모 연락처를 null로 저장한다")
    void updateStudent_blankParentPhone_storesNull() {
        Student student = Student.create(
                100L,
                "01099999999",
                "김민조",
                "01099999999",
                "양천고",
                2000,
                "01012345678",
                null
        );
        ReflectionTestUtils.setField(student, "id", 42L);
        given(studentRepository.findById(42L)).willReturn(Optional.of(student));
        given(studentRepository.save(any(Student.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.updateStudent(42L, new com.lumie.student.application.dto.request.StudentUpdateRequest(
                null,
                null,
                null,
                null,
                "",
                null
        ));

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getParentPhone()).isNull();
    }

    @Test
    @DisplayName("updateStudent는 잘못된 학부모 연락처 형식을 막는다")
    void updateStudent_invalidParentPhone_blocksUpdate() {
        Student student = Student.create(
                100L,
                "01099999999",
                "김민조",
                "01099999999",
                "양천고",
                2000,
                "01012345678",
                null
        );
        ReflectionTestUtils.setField(student, "id", 42L);
        given(studentRepository.findById(42L)).willReturn(Optional.of(student));

        assertThatThrownBy(() -> service.updateStudent(42L, new com.lumie.student.application.dto.request.StudentUpdateRequest(
                null,
                null,
                null,
                null,
                "---",
                null
        )))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.INVALID_PHONE_NUMBER));

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("registerStudent는 학생 연락처 중복을 막는다")
    void registerStudent_duplicateStudentPhone_blocksRegistration() {
        StudentRequest request = new StudentRequest(
                "김민조",
                "01012345678",
                "양천고",
                2000,
                "01088888888",
                null
        );
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));
        given(studentRepository.existsByPhone("01012345678")).willReturn(true);

        assertThatThrownBy(() -> service.registerStudent(request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.DUPLICATE_PHONE));

        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
    }

    private static Student inactiveStudent(Long id, Long userId) {
        Student student = Student.create(userId, "student@example.com", "홍길동", "01012345678", null, null, null, null);
        student.deactivate();
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
