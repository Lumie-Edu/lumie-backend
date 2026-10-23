package com.lumie.staff.application.service;

import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.quota.MetricType;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.staff.application.dto.request.StaffRequest;
import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.exception.StaffErrorCode;
import com.lumie.staff.domain.repository.StaffPermissionRepository;
import com.lumie.staff.domain.repository.StaffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaffCommandService 테스트")
class StaffCommandServiceTest {

    private static final String TENANT = "inst-test";

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StaffPermissionRepository staffPermissionRepository;

    @Mock
    private AuthService authService;

    @Mock
    private BillingService billingService;

    @Mock
    private ClassService classService;

    @InjectMocks
    private StaffCommandService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant(TENANT);
        TenantContextHolder.setTenantId(1L);
        UserContextHolder.setUserId(900L);
        UserContextHolder.setUserRole(Role.OWNER);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("deleteStaff는 담당 수업이 있는 직원 삭제를 막는다")
    void deleteStaff_assignedClasses_blocksDeletion() {
        Staff staff = inactiveStaff(10L, 100L);
        given(staffRepository.findById(10L)).willReturn(Optional.of(staff));
        given(classService.hasAssignedClasses(TENANT, 10L)).willReturn(true);

        assertThatThrownBy(() -> service.deleteStaff(10L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StaffErrorCode.CANNOT_DELETE_STAFF_WITH_ASSIGNED_CLASSES));

        verify(authService, never()).deleteUser(anyLong());
    }

    @Test
    @DisplayName("registerStaff는 11자리가 아닌 직원 연락처를 막는다")
    void registerStaff_tenDigitPhone_blocksRegistration() {
        StaffRequest request = new StaffRequest(
                Role.INSTRUCTOR.name(),
                "강사",
                "02-1234-5678",
                "teacher@example.com",
                null
        );
        given(billingService.checkQuota(TENANT, MetricType.STAFF))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STAFF, "ok"));

        assertThatThrownBy(() -> service.registerStaff(request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StaffErrorCode.INVALID_PHONE_NUMBER));

        verify(authService, never()).createUser(any(), any(), any(), anyLong());
    }

    @Test
    void registerStaffStoresContactEmailOnCanonicalUser() {
        var request = new StaffRequest(Role.INSTRUCTOR.name(), "강사님", "010-1234-5678",
                "teacher@example.com", null);
        given(billingService.checkQuota(TENANT, MetricType.STAFF))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STAFF, "ok"));
        given(authService.createUser("강사님", "010-1234-5678", Role.INSTRUCTOR.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "staff_100"));
        given(staffRepository.save(any(Staff.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(authService.updateIdentity(100L, null, null, "teacher@example.com"))
                .willReturn(true);

        service.registerStaff(request);

        verify(authService).updateIdentity(100L, null, null, "teacher@example.com");
    }

    @Test
    void registerStaffWithoutPhoneReturnsNullPhone() {
        var request = new StaffRequest(Role.INSTRUCTOR.name(), "강사님", "", null, null);
        given(billingService.checkQuota(TENANT, MetricType.STAFF))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STAFF, "ok"));
        given(authService.createUser("강사님", "", Role.INSTRUCTOR.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "staff_100"));
        given(staffRepository.save(any(Staff.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.registerStaff(request).phone()).isNull();
    }

    @Test
    void managerCanCreateInstructorButCannotCreateAnotherManager() {
        UserContextHolder.setUserRole(Role.MANAGER);
        StaffRequest request = new StaffRequest(Role.MANAGER.name(), "관리자", "", null, null);

        assertThatThrownBy(() -> service.registerStaff(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StaffErrorCode.INSUFFICIENT_PERMISSION));

        verifyNoInteractions(billingService, authService);
    }

    @Test
    void managerCanCreateInstructorAccount() {
        UserContextHolder.setUserRole(Role.MANAGER);
        StaffRequest request = new StaffRequest(Role.INSTRUCTOR.name(), "강사", "", null, null);
        given(billingService.checkQuota(TENANT, MetricType.STAFF))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STAFF, "ok"));
        given(authService.createUser("강사", "", Role.INSTRUCTOR.name(), 1L))
                .willReturn(new AuthService.CreateUserResult(true, "ok", 100L, "staff_100"));
        given(staffRepository.save(any(Staff.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.registerStaff(request)).isNotNull();

        verify(authService).createUser("강사", "", Role.INSTRUCTOR.name(), 1L);
    }

    @Test
    void managerCannotChangeManagerAccount() {
        UserContextHolder.setUserRole(Role.MANAGER);
        Staff target = Staff.create(100L, "manager_100", "관리자", "01012345678", null, null);
        ReflectionTestUtils.setField(target, "id", 10L);
        ReflectionTestUtils.setField(target, "role", Role.MANAGER.name());
        given(staffRepository.findById(10L)).willReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateStaff(10L,
                new com.lumie.staff.application.dto.request.StaffUpdateRequest(null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));

        verify(staffRepository, never()).save(any(Staff.class));
        verifyNoInteractions(authService);
    }

    @Test
    void managerCannotChangeOwnStaffAccount() {
        UserContextHolder.setUserRole(Role.MANAGER);
        Staff target = Staff.create(900L, "manager_900", "관리자", "01012345678", null, null);
        ReflectionTestUtils.setField(target, "id", 10L);
        ReflectionTestUtils.setField(target, "role", Role.MANAGER.name());
        given(staffRepository.findById(10L)).willReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateStaff(10L,
                new com.lumie.staff.application.dto.request.StaffUpdateRequest(null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));

        verify(staffRepository, never()).save(any(Staff.class));
        verifyNoInteractions(authService);
    }

    @Test
    void managerCanChangeInstructorAccount() {
        UserContextHolder.setUserRole(Role.MANAGER);
        Staff target = Staff.create(100L, "instructor_100", "강사", "01012345678", null, null);
        ReflectionTestUtils.setField(target, "id", 10L);
        ReflectionTestUtils.setField(target, "role", Role.INSTRUCTOR.name());
        given(staffRepository.findById(10L)).willReturn(Optional.of(target));
        given(staffRepository.save(target)).willReturn(target);

        service.updateStaff(10L,
                new com.lumie.staff.application.dto.request.StaffUpdateRequest(null, null, null, null));

        verify(staffRepository).save(target);
    }

    @Test
    void managerCanReactivateInstructorAccountAfterRoleValidation() {
        UserContextHolder.setUserRole(Role.MANAGER);
        Staff target = Staff.create(100L, "instructor_100", "강사", "01012345678", null, null);
        target.deactivate();
        ReflectionTestUtils.setField(target, "id", 10L);
        ReflectionTestUtils.setField(target, "role", Role.INSTRUCTOR.name());
        given(staffRepository.findById(10L)).willReturn(Optional.of(target));
        given(billingService.checkQuota(TENANT, MetricType.STAFF))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.STAFF, "ok"));
        given(staffRepository.save(target)).willReturn(target);

        service.reactivateStaff(10L);

        assertThat(target.isActive()).isTrue();
        verify(billingService).checkQuota(TENANT, MetricType.STAFF);
    }

    @Test
    void managerCanSetPermissionsForInstructorAccount() {
        UserContextHolder.setUserRole(Role.MANAGER);
        Staff target = Staff.create(100L, "instructor_100", "강사", "01012345678", null, null);
        ReflectionTestUtils.setField(target, "id", 10L);
        ReflectionTestUtils.setField(target, "role", Role.INSTRUCTOR.name());
        given(staffRepository.findById(10L)).willReturn(Optional.of(target));
        Map<String, com.lumie.staff.domain.vo.AccessLevel> permissions =
                Map.of("STUDENT_READ", com.lumie.staff.domain.vo.AccessLevel.READ);

        service.setStaffPermissions(10L, permissions);

        verify(staffPermissionRepository).deleteByStaffId(10L);
        verify(staffPermissionRepository).saveAll(anyList());
    }

    @Test
    void managerCannotChangePeerOrOwnerThroughAnyStaffMutation() {
        UserContextHolder.setUserRole(Role.MANAGER);
        for (Role targetRole : List.of(Role.MANAGER, Role.OWNER)) {
            Staff target = Staff.create(100L, "target_100", "Target", "01012345678", null, null);
            ReflectionTestUtils.setField(target, "id", 10L);
            ReflectionTestUtils.setField(target, "role", targetRole.name());
            target.deactivate();
            given(staffRepository.findById(10L)).willReturn(Optional.of(target));

            assertForbidden(() -> service.updateStaff(10L,
                    new com.lumie.staff.application.dto.request.StaffUpdateRequest(null, null, null, null)));
            assertForbidden(() -> service.setStaffPermissions(10L, Map.of()));
            assertForbidden(() -> service.resetStaffPassword(10L));
            assertForbidden(() -> service.changeStaffLoginId(10L, "new_login"));
            assertForbidden(() -> service.deactivateStaff(10L));
            assertForbidden(() -> service.reactivateStaff(10L));
            assertForbidden(() -> service.terminateStaff(10L));
            assertForbidden(() -> service.deleteStaff(10L));
        }

        verify(staffRepository, never()).save(any(Staff.class));
        verify(staffPermissionRepository, never()).deleteByStaffId(anyLong());
        verifyNoInteractions(authService, billingService, classService);
    }

    private static void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    void updateStaffBlankPhoneReturnsTheNullStoredOnUser() {
        Staff staff = Staff.create(100L, "staff_100", "강사님", "01011112222", null, null);
        ReflectionTestUtils.setField(staff, "id", 10L);
        ReflectionTestUtils.setField(staff, "role", Role.INSTRUCTOR.name());
        given(staffRepository.findById(10L)).willReturn(Optional.of(staff));
        given(staffRepository.save(staff)).willReturn(staff);
        given(authService.updateIdentity(100L, null, "", null)).willReturn(true);

        assertThat(service.updateStaff(10L,
                new com.lumie.staff.application.dto.request.StaffUpdateRequest(null, "", null, null))
                .phone()).isNull();
        verify(authService).updateIdentity(100L, null, "", null);
    }

    @Test
    void updateStaffWritesCanonicalUserIdentityAndEmail() {
        Staff staff = Staff.create(100L, "staff_100", "기존 이름", "01011112222", "old@example.com", null);
        ReflectionTestUtils.setField(staff, "id", 10L);
        ReflectionTestUtils.setField(staff, "role", Role.INSTRUCTOR.name());
        given(staffRepository.findById(10L)).willReturn(Optional.of(staff));
        given(staffRepository.save(staff)).willReturn(staff);
        given(authService.updateIdentity(100L, "새 이름", "01033334444", "new@example.com"))
                .willReturn(true);

        service.updateStaff(10L, new com.lumie.staff.application.dto.request.StaffUpdateRequest(
                "새 이름", "010-3333-4444", "new@example.com", null));

        verify(authService).updateIdentity(100L, "새 이름", "01033334444", "new@example.com");
    }

    private static Staff inactiveStaff(Long id, Long userId) {
        Staff staff = Staff.create(userId, "staff@example.com", "강사", "01012345678", null, null);
        staff.deactivate();
        ReflectionTestUtils.setField(staff, "id", id);
        ReflectionTestUtils.setField(staff, "role", Role.INSTRUCTOR.name());
        return staff;
    }
}
