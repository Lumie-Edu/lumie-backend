package com.lumie.staff.adapter.in.internal;

import com.lumie.staff.application.dto.request.StaffFilterRequest;
import com.lumie.staff.application.dto.response.StaffResponse;
import com.lumie.staff.application.service.StaffQueryService;
import com.lumie.staff.domain.entity.Staff;
import com.lumie.common.exception.BusinessException;
import com.lumie.staff.domain.exception.StaffErrorCode;
import com.lumie.staff.domain.repository.StaffPermissionRepository;
import com.lumie.staff.domain.repository.StaffRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.staff.api.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaffServiceAdapter implements StaffService {

    private final StaffQueryService staffQueryService;
    private final StaffRepository staffRepository;
    private final StaffPermissionRepository staffPermissionRepository;

    @Override
    public Optional<StaffData> getStaff(String tenantSlug, Long staffId) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            try {
                StaffResponse staff = staffQueryService.getStaff(staffId);
                return Optional.of(toStaffData(staff));
            } catch (BusinessException e) {
                if (e.getErrorCode() == StaffErrorCode.STAFF_NOT_FOUND) {
                    return Optional.<StaffData>empty();
                }
                throw e;
            }
        });
    }

    @Override
    public Optional<StaffData> getStaffByUserId(String tenantSlug, Long userId) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                staffRepository.findByUserId(userId).map(this::toStaffDataFromEntity)
        );
    }

    @Override
    public ValidateStaffResult validateStaff(String tenantSlug, Long staffId) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            try {
                staffQueryService.getStaff(staffId);
                return new ValidateStaffResult(true, "Staff is valid", staffId);
            } catch (BusinessException e) {
                if (e.getErrorCode() == StaffErrorCode.STAFF_NOT_FOUND) {
                    return new ValidateStaffResult(false, "Staff not found", staffId);
                }
                throw e;
            }
        });
    }

    @Override
    public StaffPermissionsData getStaffPermissions(String tenantSlug, Long staffId) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(staffId)));

            List<PermissionEntry> permissions = staffPermissionRepository.findByStaffId(staff.getId())
                    .stream()
                    .map(sp -> new PermissionEntry(sp.getPermissionCode(), sp.getAccessLevel().name()))
                    .toList();

            return new StaffPermissionsData(staff.getId(), permissions);
        });
    }

    @Override
    public StaffData createOwnerStaff(String tenantSlug, Long userId, String userLoginId,
                                      String name, String phone, String email) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            // Idempotent: Spring Modulith may re-deliver OwnerRegisteredEvent on
            // restart if the prior listener invocation didn't mark the publication
            // complete. Returning the existing staff row keeps the operation safe.
            return staffRepository.findByUserId(userId)
                    .map(existing -> {
                        log.info("OWNER staff already exists for userId={}, tenant={} — idempotent skip",
                                userId, tenantSlug);
                        return toStaffDataFromEntity(existing);
                    })
                    .orElseGet(() -> {
                        Staff staff = Staff.create(userId, userLoginId, name, phone, email, null);
                        Staff saved = staffRepository.save(staff);
                        log.info("OWNER staff row created: userId={}, tenant={}", userId, tenantSlug);
                        return toStaffDataFromEntity(saved);
                    });
        });
    }

    @Override
    public StaffListData getAllStaff(String tenantSlug, int page, int size) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            var filter = new StaffFilterRequest(true, null);
            var pageable = PageRequest.of(page, size);
            var staffPage = staffQueryService.getAllStaff(filter, pageable);

            return new StaffListData(
                    staffPage.getContent().stream().map(this::toStaffData).toList(),
                    staffPage.getTotalElements(), page, size
            );
        });
    }

    private StaffData toStaffData(StaffResponse dto) {
        return new StaffData(
                dto.id(), dto.userId(), dto.userLoginId(), dto.name(),
                dto.phone() != null ? dto.phone() : "",
                dto.email() != null ? dto.email() : "",
                dto.isActive(),
                dto.employmentStatus() != null ? dto.employmentStatus().name() : "",
                dto.createdAt().toString(), dto.updatedAt().toString()
        );
    }

    private StaffData toStaffDataFromEntity(Staff staff) {
        return new StaffData(
                staff.getId(), staff.getUserId(), staff.getUserLoginId(), staff.getStaffName(),
                staff.getStaffPhone() != null ? staff.getStaffPhone() : "",
                staff.getEmail() != null ? staff.getEmail() : "",
                staff.isActive(),
                staff.getEmploymentStatus() != null ? staff.getEmploymentStatus().name() : "",
                staff.getCreatedAt().toString(), staff.getUpdatedAt().toString()
        );
    }
}
