package com.lumie.admin.adapter.internal;

import com.lumie.admin.application.dto.AdminFilterRequest;
import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.application.service.AdminQueryService;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.repository.AdminPermissionRepository;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.admin.api.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminServiceAdapter implements AdminService {

    private final AdminQueryService adminQueryService;
    private final AdminRepository adminRepository;
    private final AdminPermissionRepository adminPermissionRepository;

    @Override
    public Optional<AdminData> getAdmin(String tenantSlug, Long adminId) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            try {
                AdminResponse admin = adminQueryService.getAdmin(adminId);
                return Optional.of(toAdminData(admin));
            } catch (AdminNotFoundException e) {
                return Optional.<AdminData>empty();
            }
        });
    }

    @Override
    public Optional<AdminData> getAdminByUserId(String tenantSlug, Long userId) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                adminRepository.findByUserId(userId).map(this::toAdminDataFromEntity)
        );
    }

    @Override
    public ValidateAdminResult validateAdmin(String tenantSlug, Long adminId) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            try {
                adminQueryService.getAdmin(adminId);
                return new ValidateAdminResult(true, "Admin is valid", adminId);
            } catch (AdminNotFoundException e) {
                return new ValidateAdminResult(false, "Admin not found", adminId);
            }
        });
    }

    @Override
    public AdminPermissionsData getAdminPermissions(String tenantSlug, Long adminId) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            Admin admin = adminRepository.findById(adminId)
                    .orElseThrow(() -> new AdminNotFoundException(adminId));

            List<PermissionEntry> permissions = adminPermissionRepository.findByAdminId(admin.getId())
                    .stream()
                    .map(ap -> new PermissionEntry(ap.getPermissionCode(), ap.getAccessLevel().name()))
                    .toList();

            return new AdminPermissionsData(admin.getId(), permissions);
        });
    }

    @Override
    public AdminData createOwnerAdmin(String tenantSlug, Long userId, String userLoginId,
                                      String name, String phone, String email) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            Admin admin = Admin.create(userId, userLoginId, name, phone, email, null);
            Admin saved = adminRepository.save(admin);
            log.info("OWNER admin row created: userId={}, tenant={}", userId, tenantSlug);
            return toAdminDataFromEntity(saved);
        });
    }

    @Override
    public AdminListData getAllAdmins(String tenantSlug, int page, int size) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            var filter = new AdminFilterRequest(true, null);
            var pageable = PageRequest.of(page, size);
            var admins = adminQueryService.getAllAdmins(filter, pageable);

            return new AdminListData(
                    admins.getContent().stream().map(this::toAdminData).toList(),
                    admins.getTotalElements(), page, size
            );
        });
    }

    private AdminData toAdminData(AdminResponse dto) {
        return new AdminData(
                dto.id(), dto.userId(), dto.userLoginId(), dto.name(),
                dto.phone() != null ? dto.phone() : "",
                dto.email() != null ? dto.email() : "",
                dto.isActive(),
                dto.employmentStatus() != null ? dto.employmentStatus().name() : "",
                dto.createdAt().toString(), dto.updatedAt().toString()
        );
    }

    private AdminData toAdminDataFromEntity(Admin admin) {
        return new AdminData(
                admin.getId(), admin.getUserId(), admin.getUserLoginId(), admin.getAdminName(),
                admin.getAdminPhone() != null ? admin.getAdminPhone() : "",
                admin.getEmail() != null ? admin.getEmail() : "",
                admin.isActive(),
                admin.getEmploymentStatus() != null ? admin.getEmploymentStatus().name() : "",
                admin.getCreatedAt().toString(), admin.getUpdatedAt().toString()
        );
    }
}
