package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.AdminRequest;
import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.application.port.out.AuthServicePort;
import com.lumie.admin.application.port.out.BillingServicePort;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.entity.AdminPermission;
import com.lumie.admin.domain.entity.Position;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.exception.DuplicatePhoneException;
import com.lumie.admin.domain.exception.DuplicateUserLoginIdException;
import com.lumie.admin.domain.exception.PositionNotFoundException;
import com.lumie.admin.domain.exception.QuotaExceededException;
import com.lumie.admin.domain.repository.AdminPermissionRepository;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.admin.domain.repository.PositionRepository;
import com.lumie.admin.domain.vo.AccessLevel;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCommandService {

    private final AdminRepository adminRepository;
    private final PositionRepository positionRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final AuthServicePort authServicePort;
    private final BillingServicePort billingServicePort;

    public AdminResponse registerAdmin(Long userId, AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        checkAdminQuota(tenantSlug);

        if (adminRepository.existsByUserLoginId(request.userLoginId())) {
            throw new DuplicateUserLoginIdException(request.userLoginId());
        }

        if (request.phone() != null && adminRepository.existsByPhone(request.phone())) {
            throw new DuplicatePhoneException(request.phone());
        }

        Set<Long> academyIds = new HashSet<>();
        if (request.academyIds() != null) {
            academyIds.addAll(request.academyIds());
        }

        Position position = null;
        if (request.positionId() != null) {
            position = positionRepository.findById(request.positionId())
                    .orElseThrow(() -> new PositionNotFoundException(request.positionId()));
        }

        Admin admin = Admin.create(
            userId,
            request.userLoginId(),
            request.name(),
            request.phone(),
            request.email(),
            academyIds,
            position,
            request.adminMemo()
        );

        Admin saved = adminRepository.save(admin);

        log.info("Admin registered: {} in tenant: {}", saved.getUserLoginId(), tenantSlug);
        return AdminResponse.from(saved);
    }

    public AdminResponse updateAdmin(Long id, AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        if (request.phone() != null && adminRepository.existsByPhoneAndIdNot(request.phone(), id)) {
            throw new DuplicatePhoneException(request.phone());
        }

        Position position = null;
        if (request.positionId() != null) {
            position = positionRepository.findById(request.positionId())
                    .orElseThrow(() -> new PositionNotFoundException(request.positionId()));
        }

        admin.updateInfo(request.name(), request.phone(), request.email(), position,
                request.adminMemo());

        if (request.academyIds() != null) {
            admin.setAcademyIds(new HashSet<>(request.academyIds()));
        }

        Admin updated = adminRepository.save(admin);

        log.info("Admin updated: {} in tenant: {}", id, tenantSlug);
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
    }

    public void deactivateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        admin.deactivate();
        adminRepository.save(admin);
        log.info("Admin deactivated: {} in tenant: {}", id, tenantSlug);
    }

    public void reactivateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        checkAdminQuota(tenantSlug);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        admin.activate();
        adminRepository.save(admin);
        log.info("Admin reactivated: {} in tenant: {}", id, tenantSlug);
    }

    public void terminateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        admin.terminate();
        adminRepository.save(admin);
        log.info("Admin terminated: {} in tenant: {}", id, tenantSlug);
    }

    public void deleteAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        if (admin.isActive()) {
            throw new IllegalStateException("Cannot delete active admin. Deactivate first.");
        }
        Long userId = admin.getUserId();
        adminRepository.delete(admin);
        AuthServicePort.DeleteUserResult deleteResult = authServicePort.deleteUser(userId);
        if (!deleteResult.success()) {
            log.warn("Failed to delete user from auth-svc: {}", deleteResult.message());
        }
        log.info("Admin permanently deleted: {} in tenant: {}", id, tenantSlug);
    }

    private void checkAdminQuota(String tenantSlug) {
        var result = billingServicePort.checkQuota(tenantSlug, "MAX_ADMINS");
        if (!result.allowed()) {
            throw new QuotaExceededException("MAX_ADMINS", result.currentUsage(), result.limit());
        }
    }
}
