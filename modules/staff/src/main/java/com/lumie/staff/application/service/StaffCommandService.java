package com.lumie.staff.application.service;

import com.lumie.staff.application.dto.request.StaffRequest;
import com.lumie.staff.application.dto.request.StaffUpdateRequest;
import com.lumie.staff.application.dto.response.StaffResponse;
import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.classroom.api.ClassService;
import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.entity.StaffPermission;
import com.lumie.common.quota.MetricType;
import com.lumie.common.exception.QuotaExceededException;
import com.lumie.staff.domain.exception.StaffErrorCode;
import com.lumie.staff.domain.repository.StaffPermissionRepository;
import com.lumie.staff.domain.repository.StaffRepository;
import com.lumie.staff.domain.vo.AccessLevel;
import com.lumie.common.auth.Role;
import com.lumie.common.cache.CacheConfig;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StaffCommandService {

    /** Roles a staff member can be created with via the staff module (OWNER is system-managed). */
    private static final EnumSet<Role> CREATABLE_ROLES = EnumSet.of(Role.MANAGER, Role.INSTRUCTOR);

    private final StaffRepository staffRepository;
    private final StaffPermissionRepository staffPermissionRepository;
    private final AuthService authService;
    private final BillingService billingService;
    private final ClassService classService;

    public StaffResponse registerStaff(StaffRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Role creatorRole = UserContextHolder.getUserRole();

        String requestedRole = request.role().toUpperCase(Locale.ROOT);
        validateRolePermission(creatorRole, requestedRole);

        checkStaffQuota(tenantSlug);
        validateStaffPhoneFormat(request.phone());

        String normalizedPhone = PhoneNumberUtils.toDigits(request.phone());
        if (normalizedPhone != null && !normalizedPhone.isBlank() && staffRepository.existsByCanonicalPhone(normalizedPhone)) {
            throw new BusinessException(StaffErrorCode.DUPLICATE_PHONE, "Phone number already exists: %s".formatted(request.phone()));
        }

        AuthService.CreateUserResult userResult = authService.createUser(
                request.name(),
                request.phone(),
                requestedRole,
                tenantId
        );

        if (!userResult.success()) {
            log.error("Auth service createUser failed: {}", userResult.message());
            throw new BusinessException(StaffErrorCode.AUTH_OP_FAILED);
        }

        Staff staff = Staff.create(
            userResult.userId(),
            userResult.userLoginId(),
            request.name(),
            request.phone(),
            request.email(),
            request.staffMemo()
        );

        Staff saved = staffRepository.save(staff);
        if (request.email() != null && !authService.updateIdentity(
                userResult.userId(), null, null, request.email())) {
            throw new BusinessException(StaffErrorCode.AUTH_OP_FAILED, "Linked user not found");
        }

        log.info("Staff registered: {} (role={}) in tenant: {}", saved.getUserLoginId(), requestedRole, tenantSlug);
        return StaffResponse.from(saved);
    }

    public StaffResponse updateStaff(Long id, StaffUpdateRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(id)));

        validateActorAuthority(staff, "update");

        validateStaffPhoneFormat(request.phone());
        String normalizedPhone = PhoneNumberUtils.toDigits(request.phone());
        if (normalizedPhone != null && !normalizedPhone.isBlank() && staffRepository.existsByCanonicalPhoneAndIdNot(normalizedPhone, id)) {
            throw new BusinessException(StaffErrorCode.DUPLICATE_PHONE, "Phone number already exists: %s".formatted(request.phone()));
        }

        staff.updateInfo(request.name(), request.phone(), request.email(),
                request.staffMemo());

        Staff updated = staffRepository.save(staff);
        if ((request.name() != null || request.phone() != null || request.email() != null)
                && !authService.updateIdentity(staff.getUserId(), request.name(),
                        PhoneNumberUtils.toDigits(request.phone()), request.email())) {
            throw new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Linked user not found");
        }

        log.info("Staff updated: {} in tenant: {}", id, tenantSlug);
        return StaffResponse.from(updated);
    }

    @CacheEvict(value = CacheConfig.CACHE_ADMIN_PERMISSIONS, key = "#staffId")
    public void setStaffPermissions(Long staffId, Map<String, AccessLevel> permissions) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(staffId)));

        validateActorAuthority(staff, "modify permissions of");

        staffPermissionRepository.deleteByStaffId(staffId);

        List<StaffPermission> entries = permissions.entrySet().stream()
                .map(e -> StaffPermission.of(staffId, e.getKey(), e.getValue()))
                .toList();
        staffPermissionRepository.saveAll(entries);

        log.info("Staff {} permissions updated: {} entries", staffId, entries.size());
    }

    public void deactivateStaff(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(id)));
        validateActorAuthority(staff, "deactivate");
        staff.deactivate();
        staffRepository.save(staff);
        log.info("Staff deactivated: {} in tenant: {}", id, tenantSlug);
    }

    public void reactivateStaff(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        checkStaffQuota(tenantSlug);
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(id)));
        validateActorAuthority(staff, "reactivate");
        staff.activate();
        staffRepository.save(staff);
        log.info("Staff reactivated: {} in tenant: {}", id, tenantSlug);
    }

    public void terminateStaff(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(id)));
        validateActorAuthority(staff, "terminate");
        staff.terminate();
        staffRepository.save(staff);
        log.info("Staff terminated: {} in tenant: {}", id, tenantSlug);
    }

    public void resetStaffPassword(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(id)));

        validateActorAuthority(staff, "reset password for");

        AuthService.ResetPasswordResult result = authService.resetPassword(staff.getUserId());
        if (!result.success()) {
            log.error("Auth service resetPassword failed for staffId={}: {}", id, result.message());
            throw new BusinessException(StaffErrorCode.AUTH_OP_FAILED);
        }

        log.info("Password reset for staff: {} in tenant: {}", id, tenantSlug);
    }

    public String changeStaffLoginId(Long id, String newLoginId) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(id)));

        validateActorAuthority(staff, "change login id for");

        AuthService.ChangeLoginIdResult result = authService.changeLoginId(staff.getUserId(), newLoginId);
        if (!result.success()) {
            log.error("Auth service changeLoginId failed for staffId={}: {}", id, result.message());
            throw new BusinessException(StaffErrorCode.AUTH_OP_FAILED);
        }

        staff.updateLoginId(result.newLoginId());
        staffRepository.save(staff);

        log.info("Login ID changed for staff: {} in tenant: {}", id, tenantSlug);
        return result.newLoginId();
    }

    public void deleteStaff(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(id)));
        validateActorAuthority(staff, "delete");
        if (staff.isActive()) {
            throw new BusinessException(StaffErrorCode.CANNOT_DELETE_ACTIVE_STAFF);
        }
        validateNoCurrentOrFutureOperations(staff, tenantSlug);
        Long userId = staff.getUserId();
        staffRepository.delete(staff);
        AuthService.DeleteUserResult deleteResult = authService.deleteUser(userId);
        if (!deleteResult.success()) {
            log.warn("Failed to delete user from auth-svc: {}", deleteResult.message());
        }
        log.info("Staff permanently deleted: {} in tenant: {}", id, tenantSlug);
    }

    private void validateActorAuthority(Staff target, String action) {
        Long actorUserId = UserContextHolder.getRequiredUserId();
        if (target.getUserId().equals(actorUserId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN,
                    "Cannot " + action + " yourself");
        }
        Role actor = UserContextHolder.getUserRole();
        Role targetRole = Role.valueOf(target.getRole());
        if (!actor.canManage(targetRole)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN,
                    actor + " cannot " + action + " " + targetRole);
        }
    }

    private void validateStaffPhoneFormat(String phone) {
        if (phone != null && !phone.isBlank() && !PhoneNumberUtils.isValidStudentPhone(phone)) {
            throw new BusinessException(
                    StaffErrorCode.INVALID_PHONE_NUMBER,
                    "Invalid staff phone number format: %s".formatted(phone));
        }
    }

    private void validateRolePermission(Role creator, String requestedRole) {
        Role requested;
        try {
            requested = Role.valueOf(requestedRole);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(StaffErrorCode.INVALID_ROLE,
                    "Invalid role: " + requestedRole + ". Allowed: MANAGER, INSTRUCTOR");
        }
        if (!CREATABLE_ROLES.contains(requested)) {
            throw new BusinessException(StaffErrorCode.INVALID_ROLE,
                    "Invalid role: " + requestedRole + ". Allowed: MANAGER, INSTRUCTOR");
        }
        if (!creator.canManage(requested)) {
            throw new BusinessException(StaffErrorCode.INSUFFICIENT_PERMISSION,
                    "Insufficient permission to create " + requested);
        }
    }

    private void validateNoCurrentOrFutureOperations(Staff staff, String tenantSlug) {
        if (classService.hasAssignedClasses(tenantSlug, staff.getId())) {
            throw new BusinessException(
                    StaffErrorCode.CANNOT_DELETE_STAFF_WITH_ASSIGNED_CLASSES,
                    "담당 중인 수업이 있어 직원을 삭제할 수 없습니다. 먼저 수업 담당자를 변경하거나 미배정으로 바꿔 주세요.");
        }
    }

    private void checkStaffQuota(String tenantSlug) {
        var result = billingService.checkQuota(tenantSlug, MetricType.ADMINS);
        if (!result.allowed()) {
            throw new QuotaExceededException(
                StaffErrorCode.QUOTA_EXCEEDED,
                TenantContextHolder.getTenant(),
                MetricType.ADMINS,
                result.currentUsage(),
                result.limit()
            );
        }
    }
}
