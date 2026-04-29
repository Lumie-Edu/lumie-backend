package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.AdminRequest;
import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.application.port.out.AuthServicePort;
import com.lumie.admin.application.port.out.BillingServicePort;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.entity.AdminPermission;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.exception.DuplicatePhoneException;
import com.lumie.admin.domain.exception.QuotaExceededException;
import com.lumie.admin.domain.repository.AdminPermissionRepository;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.admin.domain.vo.AccessLevel;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.internal.activitylog.ActivityLogInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCommandService {

    private static final Set<String> ALLOWED_ROLES = Set.of("MANAGER", "INSTRUCTOR");

    private final AdminRepository adminRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final AuthServicePort authServicePort;
    private final BillingServicePort billingServicePort;
    private final ActivityLogInternalService activityLogInternalService;

    public AdminResponse registerAdmin(AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String creatorRole = UserContextHolder.getUserRole();

        String requestedRole = request.role().toUpperCase();
        validateRolePermission(creatorRole, requestedRole);

        checkAdminQuota(tenantSlug);

        if (request.phone() != null && adminRepository.existsByPhone(request.phone())) {
            throw new DuplicatePhoneException(request.phone());
        }

        AuthServicePort.CreateUserResult userResult = authServicePort.createUser(
                new AuthServicePort.CreateUserRequest(
                        request.name(),
                        request.phone(),
                        requestedRole,
                        tenantId
                )
        );

        if (!userResult.success()) {
            throw new RuntimeException("Failed to create user: " + userResult.message());
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
        logActivity("CREATE", "ADMIN", saved.getId(), saved.getName(), null);
        return AdminResponse.from(saved);
    }

    public AdminResponse updateAdmin(Long id, AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        if (request.phone() != null && adminRepository.existsByPhoneAndIdNot(request.phone(), id)) {
            throw new DuplicatePhoneException(request.phone());
        }

        admin.updateInfo(request.name(), request.phone(), request.email(),
                request.adminMemo());

        Admin updated = adminRepository.save(admin);

        log.info("Admin updated: {} in tenant: {}", id, tenantSlug);
        logActivity("UPDATE", "ADMIN", updated.getId(), updated.getName(), null);
        return AdminResponse.from(updated);
    }

    public void setAdminPermissions(Long adminId, Map<String, String> permissions) {
        adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException(adminId));

        adminPermissionRepository.deleteByAdminId(adminId);

        List<AdminPermission> entries = permissions.entrySet().stream()
                .map(e -> AdminPermission.of(adminId, e.getKey(), AccessLevel.valueOf(e.getValue())))
                .toList();
        adminPermissionRepository.saveAll(entries);

        log.info("Admin {} permissions updated: {} entries", adminId, entries.size());
        logActivity("UPDATE", "PERMISSION", adminId, null, entries.size() + " permissions updated");
    }

    public void deactivateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        admin.deactivate();
        adminRepository.save(admin);
        log.info("Admin deactivated: {} in tenant: {}", id, tenantSlug);
        logActivity("DEACTIVATE", "ADMIN", id, admin.getName(), null);
    }

    public void reactivateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        checkAdminQuota(tenantSlug);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        admin.activate();
        adminRepository.save(admin);
        log.info("Admin reactivated: {} in tenant: {}", id, tenantSlug);
        logActivity("REACTIVATE", "ADMIN", id, admin.getName(), null);
    }

    public void terminateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        admin.terminate();
        adminRepository.save(admin);
        log.info("Admin terminated: {} in tenant: {}", id, tenantSlug);
        logActivity("TERMINATE", "ADMIN", id, admin.getName(), null);
    }

    public void resetAdminPassword(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        AuthServicePort.ResetPasswordResult result = authServicePort.resetPassword(admin.getUserId());
        if (!result.success()) {
            throw new RuntimeException("Failed to reset password: " + result.message());
        }

        log.info("Password reset for admin: {} in tenant: {}", id, tenantSlug);
        logActivity("RESET_PASSWORD", "ADMIN", id, admin.getName(), null);
    }

    public String changeAdminLoginId(Long id, String newLoginId) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        AuthServicePort.ChangeLoginIdResult result = authServicePort.changeLoginId(admin.getUserId(), newLoginId);
        if (!result.success()) {
            throw new RuntimeException("Failed to change login ID: " + result.message());
        }

        admin.updateLoginId(result.newLoginId());
        adminRepository.save(admin);

        log.info("Login ID changed for admin: {} in tenant: {}", id, tenantSlug);
        logActivity("CHANGE_LOGIN_ID", "ADMIN", id, admin.getName(), null);
        return result.newLoginId();
    }

    public void deleteAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        if (admin.isActive()) {
            throw new IllegalStateException("Cannot delete active admin. Deactivate first.");
        }
        Long userId = admin.getUserId();
        String adminName = admin.getName();
        adminRepository.delete(admin);
        AuthServicePort.DeleteUserResult deleteResult = authServicePort.deleteUser(userId);
        if (!deleteResult.success()) {
            log.warn("Failed to delete user from auth-svc: {}", deleteResult.message());
        }
        log.info("Admin permanently deleted: {} in tenant: {}", id, tenantSlug);
        logActivity("DELETE", "ADMIN", id, adminName, null);
    }

    private void logActivity(String action, String entityType, Long entityId,
                             String entityName, String description) {
        try {
            Long userId = UserContextHolder.getRequiredUserId();
            String role = UserContextHolder.getUserRole();
            adminRepository.findByUserId(userId).ifPresent(actor ->
                activityLogInternalService.log(actor.getId(), actor.getName(), role,
                    action, entityType, entityId, entityName, description)
            );
        } catch (Exception e) {
            log.warn("Failed to log activity: {}", e.getMessage());
        }
    }

    private void validateRolePermission(String creatorRole, String requestedRole) {
        if (!ALLOWED_ROLES.contains(requestedRole)) {
            throw new IllegalArgumentException("Invalid role: " + requestedRole + ". Allowed: MANAGER, INSTRUCTOR");
        }
        if ("OWNER".equals(creatorRole)) {
            return;
        }
        if ("MANAGER".equals(creatorRole) && "INSTRUCTOR".equals(requestedRole)) {
            return;
        }
        throw new IllegalStateException("Insufficient permission to create " + requestedRole);
    }

    private void checkAdminQuota(String tenantSlug) {
        var result = billingServicePort.checkQuota(tenantSlug, "MAX_ADMINS");
        if (!result.allowed()) {
            throw new QuotaExceededException("MAX_ADMINS", result.currentUsage(), result.limit());
        }
    }
}
