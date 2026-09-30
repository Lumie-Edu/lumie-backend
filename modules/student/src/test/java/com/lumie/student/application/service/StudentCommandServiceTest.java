package com.lumie.student.application.service;

import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.auth.Role;
import com.lumie.common.quota.MetricType;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.application.dto.request.StudentRequest;
import com.lumie.student.application.dto.response.BulkImportResult;
import com.lumie.student.api.StudentRegisteredEvent;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
        given(studentRepository.existsByCanonicalPhone("01099999999")).willReturn(false);
        given(authService.createUser("김민조", "01099999999", Role.STUDENT.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "01099999999"));
        given(studentRepository.save(any(Student.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.registerStudent(request);

        verify(studentRepository).existsByCanonicalPhone("01099999999");
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
        given(studentRepository.existsByCanonicalPhone("01099999999")).willReturn(false);
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
        given(studentRepository.existsByCanonicalPhone("01099999999")).willReturn(false);

        assertThatThrownBy(() -> service.registerStudent(request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.INVALID_PHONE_NUMBER));

        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("registerStudent는 11자리가 아닌 학생 연락처를 막는다")
    void registerStudent_tenDigitStudentPhone_blocksRegistration() {
        StudentRequest request = new StudentRequest(
                "김민조",
                "02-1234-5678",
                "양천고",
                2000,
                "01012345678",
                null
        );
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));

        assertThatThrownBy(() -> service.registerStudent(request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.INVALID_PHONE_NUMBER));

        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
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
        org.mockito.Mockito.verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("updateStudent는 빈 학생 연락처를 Student와 User 양쪽에서 삭제한다")
    void updateStudent_blankPhone_clearsCanonicalIdentity() {
        Student student = Student.create(100L, "user_100", "기존 이름", "01099999999", null, null, null, null);
        ReflectionTestUtils.setField(student, "id", 42L);
        given(studentRepository.findById(42L)).willReturn(Optional.of(student));
        given(studentRepository.save(student)).willReturn(student);
        given(authService.updateIdentity(100L, null, "", null)).willReturn(true);

        service.updateStudent(42L, new com.lumie.student.application.dto.request.StudentUpdateRequest(
                null, "", null, null, null, null));

        assertThat(student.getPhone()).isNull();
        verify(authService).updateIdentity(100L, null, "", null);
    }

    @Test
    void updateStudentNameDoesNotSendUnchangedPhoneToUser() {
        Student student = Student.create(100L, "user_100", "기존 이름", "01099999999", null, null, null, null);
        ReflectionTestUtils.setField(student, "id", 42L);
        given(studentRepository.findById(42L)).willReturn(Optional.of(student));
        given(studentRepository.save(student)).willReturn(student);
        given(authService.updateIdentity(100L, "새 이름", null, null)).willReturn(true);

        service.updateStudent(42L, new com.lumie.student.application.dto.request.StudentUpdateRequest(
                "새 이름", null, null, null, null, null));

        verify(authService).updateIdentity(100L, "새 이름", null, null);
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
        given(studentRepository.existsByCanonicalPhone("01012345678")).willReturn(true);

        assertThatThrownBy(() -> service.registerStudent(request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.DUPLICATE_PHONE));

        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
    }

    @Test
    @DisplayName("registerStudent는 비활성 학생 전화번호를 명시적으로 안내한다")
    void registerStudent_inactiveStudentPhone_reportsInactiveRecord() {
        StudentRequest request = new StudentRequest(
                "김민조",
                "01012345678",
                "양천고",
                2000,
                "01088888888",
                null
        );
        Student inactiveStudent = Student.builder()
                .name("퇴원생")
                .phone("01012345678")
                .isActive(false)
                .build();
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));
        given(studentRepository.existsByCanonicalPhone("01012345678")).willReturn(true);
        given(studentRepository.findByCanonicalPhone("01012345678")).willReturn(java.util.Optional.of(inactiveStudent));

        assertThatThrownBy(() -> service.registerStudent(request))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.INACTIVE_STUDENT_PHONE);
                    assertThat(e.getMessage()).isEqualTo("이 전화번호는 퇴원 학생 기록에 존재합니다.");
                });

        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
    }

    @Test
    @DisplayName("updateStudent는 퇴원 학생 전화번호를 명시적으로 안내한다")
    void updateStudent_inactiveStudentPhone_reportsInactiveRecord() {
        Student student = Student.create(
                100L,
                "student@example.com",
                "김민조",
                "01099999999",
                "양천고",
                2000,
                "01088888888",
                null
        );
        Student inactiveStudent = inactiveStudent(43L, 101L);
        ReflectionTestUtils.setField(student, "id", 42L);
        given(studentRepository.findById(42L)).willReturn(Optional.of(student));
        given(studentRepository.existsByCanonicalPhoneExcluding("01012345678", 42L)).willReturn(true);
        given(studentRepository.findByCanonicalPhone("01012345678")).willReturn(Optional.of(inactiveStudent));

        assertThatThrownBy(() -> service.updateStudent(42L, new com.lumie.student.application.dto.request.StudentUpdateRequest(
                null,
                "01012345678",
                null,
                null,
                null,
                null
        )))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StudentErrorCode.INACTIVE_STUDENT_PHONE));

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("bulkImportStudents는 스프레드시트 수식으로 해석될 수 있는 값을 row 오류로 처리한다")
    void bulkImportStudents_unsafeSpreadsheetValue_returnsRowError() {
        String csv = """
                학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모
                =CMD(),010-1234-5678,,,,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                csv.getBytes(UTF_8)
        );
        given(studentRepository.existsByCanonicalPhone("01012345678")).willReturn(false);

        BulkImportResult result = service.bulkImportStudents(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.errors())
                .extracting(BulkImportResult.RowError::code)
                .containsExactly(StudentErrorCode.BULK_IMPORT_UNSAFE_SPREADSHEET_VALUE.getCode());
        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("bulkImportStudents는 같은 파일 안의 중복 학생 연락처를 row 오류로 처리한다")
    void bulkImportStudents_duplicatePhoneInSameFile_returnsRowErrorBeforeAuth() {
        String csv = """
                학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모
                김민조,010-1234-5678,,,,
                이민조,010-1234-5678,,,,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                csv.getBytes(UTF_8)
        );
        given(studentRepository.existsByCanonicalPhone("01012345678")).willReturn(false);
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));
        given(authService.createUser("김민조", "010-1234-5678", Role.STUDENT.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "01012345678"));
        given(studentRepository.save(any(Student.class))).willAnswer(invocation -> invocation.getArgument(0));

        BulkImportResult result = service.bulkImportStudents(file);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.errors())
                .extracting(BulkImportResult.RowError::rowNumber, BulkImportResult.RowError::code)
                .containsExactly(tuple(3, StudentErrorCode.BULK_IMPORT_DUPLICATE_PHONE.getCode()));
    }

    @Test
    @DisplayName("bulkImportStudents는 퇴원 학생 연락처를 명시적인 row 오류로 처리한다")
    void bulkImportStudents_inactiveStudentPhone_returnsExplicitRowError() {
        String csv = """
                학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모
                김민조,010-1234-5678,,,,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                csv.getBytes(UTF_8)
        );
        given(studentRepository.existsByCanonicalPhone("01012345678")).willReturn(true);
        given(studentRepository.findByCanonicalPhone("01012345678")).willReturn(Optional.of(inactiveStudent(43L, 101L)));

        BulkImportResult result = service.bulkImportStudents(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.errors())
                .extracting(BulkImportResult.RowError::rowNumber, BulkImportResult.RowError::code,
                        BulkImportResult.RowError::message)
                .containsExactly(tuple(
                        2,
                        StudentErrorCode.BULK_IMPORT_INACTIVE_STUDENT_PHONE.getCode(),
                        "이 전화번호는 퇴원 학생 기록에 존재합니다."
                ));
        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
    }

    @Test
    @DisplayName("bulkImportStudents는 성공 row마다 학생 등록 event를 발행한다")
    void bulkImportStudents_successRows_publishRegisteredEvents() {
        String csv = """
                학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모
                김민조,010-1234-5678,,,,
                이민조,010-9999-9999,,,,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                csv.getBytes(UTF_8)
        );
        given(studentRepository.existsByCanonicalPhone("01012345678")).willReturn(false);
        given(studentRepository.existsByCanonicalPhone("01099999999")).willReturn(false);
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));
        given(authService.createUser("김민조", "010-1234-5678", Role.STUDENT.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "01012345678"));
        given(authService.createUser("이민조", "010-9999-9999", Role.STUDENT.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 101L, "01099999999"));
        given(studentRepository.save(any(Student.class))).willAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            ReflectionTestUtils.setField(student, "id", student.getUserId() + 1_000L);
            return student;
        });

        BulkImportResult result = service.bulkImportStudents(file);

        assertThat(result.successCount()).isEqualTo(2);
        ArgumentCaptor<StudentRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(StudentRegisteredEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(
                        StudentRegisteredEvent::studentId,
                        StudentRegisteredEvent::phone,
                        StudentRegisteredEvent::tenantSlug,
                        StudentRegisteredEvent::tenantId
                )
                .containsExactly(
                        tuple(1_100L, "01012345678", TENANT, 1L),
                        tuple(1_101L, "01099999999", TENANT, 1L)
                );
    }

    @Test
    @DisplayName("bulkImportStudents는 11자리가 아닌 학생 연락처를 row 오류로 처리한다")
    void bulkImportStudents_tenDigitStudentPhone_returnsRowError() {
        String csv = """
                학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모
                김민조,02-1234-5678,,,,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                csv.getBytes(UTF_8)
        );

        BulkImportResult result = service.bulkImportStudents(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.errors())
                .extracting(BulkImportResult.RowError::rowNumber, BulkImportResult.RowError::code)
                .containsExactly(tuple(2, StudentErrorCode.BULK_IMPORT_INVALID_PHONE.getCode()));
        verify(authService, never()).createUser(any(), any(), eq(Role.STUDENT.name()), anyLong());
    }

    @Test
    void bulkImportStudentPhoneAcceptsLongFormattingWhenDigitsAreValid() {
        String csv = "학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모\n"
                + "김민조,((010))---1234---5678,,,,\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "students.csv", "text/csv", csv.getBytes(UTF_8));
        given(studentRepository.existsByCanonicalPhone("01012345678")).willReturn(false);
        given(billingService.checkQuota(TENANT, MetricType.STUDENTS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STUDENTS, "ok"));
        given(authService.createUser("김민조", "((010))---1234---5678", Role.STUDENT.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "01012345678"));
        given(studentRepository.save(any(Student.class))).willAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            ReflectionTestUtils.setField(student, "id", 1_100L);
            return student;
        });

        BulkImportResult result = service.bulkImportStudents(file);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    private static Student inactiveStudent(Long id, Long userId) {
        Student student = Student.create(userId, "student@example.com", "홍길동", "01012345678", null, null, null, null);
        student.deactivate();
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
