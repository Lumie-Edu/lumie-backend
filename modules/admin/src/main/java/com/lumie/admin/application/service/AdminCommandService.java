package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.AdminRequest;
import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.application.dto.AdminUpdateRequest;
import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.entity.AdminPermission;
import com.lumie.admin.domain.exception.AdminErrorCode;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.exception.DuplicatePhoneException;
import com.lumie.admin.domain.exception.QuotaExceededException;
import com.lumie.admin.domain.repository.AdminPermissionRepository;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.admin.domain.vo.AccessLevel;
import com.lumie.common.activitylog.LogActivity;
import com.lumie.common.auth.ActionType;
import com.lumie.common.auth.EntityType;
import com.lumie.common.auth.Role;
import com.lumie.common.cache.CacheConfig;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCommandService {

    /** Roles an admin can be created with via the admin module (OWNER is system-managed). */
    private static final EnumSet<Role> CREATABLE_ROLES = EnumSet.of(Role.MANAGER, Role.INSTRUCTOR);

    private final AdminRepository adminRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final AuthService authService;
    private final BillingService billingService;

    @LogActivity(action = ActionType.CREATE, entity = EntityType.ADMIN)
    public AdminResponse registerAdmin(AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Role creatorRole = UserContextHolder.getUserRole();

        String requestedRole = request.role().toUpperCase();
        validateRolePermission(creatorRole, requestedRole);

        checkAdminQuota(tenantSlug);

        if (request.phone() != null && adminRepository.existsByPhone(request.phone())) {
            throw new DuplicatePhoneException(request.phone());
        }

        AuthService.CreateUserResult userResult = authService.createUser(
                request.name(),
                request.phone(),
                requestedRole,
                tenantId
        );

        if (!userResult.success()) {
            log.error("Auth service createUser failed: {}", userResult.message());
            throw new BusinessException(AdminErrorCode.AUTH_OP_FAILED);
        }

        Admin admin = Admin.create(
            userResult.userId(),
            userResult.userLoginId(),
            request.name(),
            request.phone(),
            request.email(),
            request.adminMemo()
        );

        Admin saved = adminRepository.save(admin);

        log.info("Admin registered: {} (role={}) in tenant: {}", saved.getUserLoginId(), requestedRole, tenantSlug);
        return AdminResponse.from(saved);
    }

    @LogActivity(action = ActionType.UPDATE, entity = EntityType.ADMIN, entityIdArg = "id")
    public AdminResponse updateAdmin(Long id, AdminUpdateRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        validateActorAuthority(admin, "update");

        if (request.phone() != null && adminRepository.existsByPhoneAndIdNot(request.phone(), id)) {
            throw new DuplicatePhoneException(request.phone());
        }

        admin.updateInfo(request.name(), request.phone(), request.email(),
                request.adminMemo());

        Admin updated = adminRepository.save(admin);

        log.info("Admin updated: {} in tenant: {}", id, tenantSlug);
        return AdminResponse.from(updated);
    }

    @CacheEvict(value = CacheConfig.CACHE_ADMIN_PERMISSIONS, key = "#adminId")
    @LogActivity(action = ActionType.UPDATE, entity = EntityType.PERMISSION, entityIdArg = "adminId")
    public void setAdminPermissions(Long adminId, Map<String, AccessLevel> permissions) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException(adminId));

        validateActorAuthority(admin, "modify permissions of");

        adminPermissionRepository.deleteByAdminId(adminId);

        List<AdminPermission> entries = permissions.entrySet().stream()
                .map(e -> AdminPermission.of(adminId, e.getKey(), e.getValue()))
                .toList();
        adminPermissionRepository.saveAll(entries);

        log.info("Admin {} permissions updated: {} entries", adminId, entries.size());
    }

    @LogActivity(action = ActionType.DEACTIVATE, entity = EntityType.ADMIN, entityIdArg = "id")
    public void deactivateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        validateActorAuthority(admin, "deactivate");
        admin.deactivate();
        adminRepository.save(admin);
        log.info("Admin deactivated: {} in tenant: {}", id, tenantSlug);
    }

    @LogActivity(action = ActionType.REACTIVATE, entity = EntityType.ADMIN, entityIdArg = "id")
    public void reactivateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        checkAdminQuota(tenantSlug);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        validateActorAuthority(admin, "reactivate");
        admin.activate();
        adminRepository.save(admin);
        log.info("Admin reactivated: {} in tenant: {}", id, tenantSlug);
    }

    @LogActivity(action = ActionType.TERMINATE, entity = EntityType.ADMIN, entityIdArg = "id")
    public void terminateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        validateActorAuthority(admin, "terminate");
        admin.terminate();
        adminRepository.save(admin);
        log.info("Admin terminated: {} in tenant: {}", id, tenantSlug);
    }

    @LogActivity(action = ActionType.RESET_PASSWORD, entity = EntityType.ADMIN, entityIdArg = "id")
    public void resetAdminPassword(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        validateActorAuthority(admin, "reset password for");

        AuthService.ResetPasswordResult result = authService.resetPassword(admin.getUserId());
        if (!result.success()) {
            log.error("Auth service resetPassword failed for adminId={}: {}", id, result.message());
            throw new BusinessException(AdminErrorCode.AUTH_OP_FAILED);
        }

        log.info("Password reset for admin: {} in tenant: {}", id, tenantSlug);
    }

    @LogActivity(action = ActionType.CHANGE_LOGIN_ID, entity = EntityType.ADMIN, entityIdArg = "id")
    public String changeAdminLoginId(Long id, String newLoginId) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        validateActorAuthority(admin, "change login id for");

        AuthService.ChangeLoginIdResult result = authService.changeLoginId(admin.getUserId(), newLoginId);
        if (!result.success()) {
            log.error("Auth service changeLoginId failed for adminId={}: {}", id, result.message());
            throw new BusinessException(AdminErrorCode.AUTH_OP_FAILED);
        }

        admin.updateLoginId(result.newLoginId());
        adminRepository.save(admin);

        log.info("Login ID changed for admin: {} in tenant: {}", id, tenantSlug);
        return result.newLoginId();
    }

    @LogActivity(action = ActionType.DELETE, entity = EntityType.ADMIN, entityIdArg = "id")
    public void deleteAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        validateActorAuthority(admin, "delete");
        if (admin.isActive()) {
            throw new IllegalStateException("Cannot delete active admin. Deactivate first.");
        }
        Long userId = admin.getUserId();
        adminRepository.delete(admin);
        AuthService.DeleteUserResult deleteResult = authService.deleteUser(userId);
        if (!deleteResult.success()) {
            log.warn("Failed to delete user from auth-svc: {}", deleteResult.message());
        }
        log.info("Admin permanently deleted: {} in tenant: {}", id, tenantSlug);
    }

    private void validateActorAuthority(Admin target, String action) {
        Long actorUserId = UserContextHolder.getRequiredUserId();
        if (target.getUserId().equals(actorUserId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN,
                    "Cannot " + action + " yourself");
        }
        Role actor = UserContextHolder.getUserRole();
        Role targetRole = Role.fromString(target.getRole());
        if (!actor.canManage(targetRole)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN,
                    actor + " cannot " + action + " " + targetRole);
        }
    }

    private void validateRolePermission(Role creator, String requestedRole) {
        Role requested = Role.fromString(requestedRole);
        if (!CREATABLE_ROLES.contains(requested)) {
            throw new IllegalArgumentException("Invalid role: " + requestedRole + ". Allowed: MANAGER, INSTRUCTOR");
        }
        if (!creator.canManage(requested)) {
            throw new IllegalStateException("Insufficient permission to create " + requested);
        }
    }

    private void checkAdminQuota(String tenantSlug) {
        var result = billingService.checkQuota(tenantSlug, "MAX_ADMINS");
        if (!result.allowed()) {
            throw new QuotaExceededException("MAX_ADMINS", result.currentUsage(), result.limit());
        }
    }
}
