package com.lumie.staff.application.service;

import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
        given(billingService.checkQuota(TENANT, MetricType.ADMINS))
                .willReturn(new BillingService.QuotaResult(true, 0L, 100L, MetricType.ADMINS, "ok"));

        assertThatThrownBy(() -> service.registerStaff(request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(StaffErrorCode.INVALID_PHONE_NUMBER));

        verify(authService, never()).createUser(any(), any(), any(), anyLong());
    }

    private static Staff inactiveStaff(Long id, Long userId) {
        Staff staff = Staff.create(userId, "staff@example.com", "강사", "01012345678", null, null);
        staff.deactivate();
        ReflectionTestUtils.setField(staff, "id", id);
        ReflectionTestUtils.setField(staff, "role", Role.INSTRUCTOR.name());
        return staff;
    }
}
