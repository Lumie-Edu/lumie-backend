package com.lumie.staff.application.service;

import com.lumie.staff.application.dto.request.StaffFilterRequest;
import com.lumie.staff.application.dto.response.StaffPermissionResponse;
import com.lumie.staff.application.dto.response.StaffResponse;
import com.lumie.staff.domain.entity.Staff;
import com.lumie.common.exception.BusinessException;
import com.lumie.staff.domain.exception.StaffErrorCode;
import com.lumie.staff.domain.repository.StaffPermissionRepository;
import com.lumie.staff.domain.repository.StaffRepository;
import com.lumie.auth.api.AuthService;
import com.lumie.common.cache.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffQueryService {

    private final StaffRepository staffRepository;
    private final StaffPermissionRepository staffPermissionRepository;
    private final AuthService authService;

    public StaffResponse getStaff(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(id)));
        return enrich(staff);
    }

    public StaffResponse getStaffByUserLoginId(String userLoginId) {
        Staff staff = staffRepository.findByUserLoginId(userLoginId)
                .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with identifier: %s".formatted(userLoginId)));
        return enrich(staff);
    }

    public Page<StaffResponse> getAllStaff(StaffFilterRequest filter, Pageable pageable) {
        Page<Staff> page = staffRepository.search(
                filter == null ? null : filter.isActive(),
                filter == null ? null : filter.search(),
                pageable);
        Map<Long, String> seeds = avatarSeedsFor(page.getContent());
        return page.map(s -> StaffResponse.from(s, seeds.get(s.getUserId())));
    }

    private StaffResponse enrich(Staff staff) {
        Map<Long, String> seeds = authService.getAvatarSeeds(List.of(staff.getUserId()));
        return StaffResponse.from(staff, seeds.get(staff.getUserId()));
    }

    private Map<Long, String> avatarSeedsFor(List<Staff> staffList) {
        if (staffList.isEmpty()) return Map.of();
        return authService.getAvatarSeeds(staffList.stream().map(Staff::getUserId).toList());
    }

    @Cacheable(value = CacheConfig.CACHE_STAFF_PERMISSIONS, key = "#staffId")
    public List<StaffPermissionResponse> getStaffPermissions(Long staffId) {
        if (staffRepository.findById(staffId).isEmpty()) {
            throw new BusinessException(StaffErrorCode.STAFF_NOT_FOUND, "Staff not found with id: %d".formatted(staffId));
        }
        return staffPermissionRepository.findByStaffId(staffId).stream()
                .map(StaffPermissionResponse::from)
                .toList();
    }

    public boolean existsByUserLoginId(String userLoginId) {
        return staffRepository.existsByUserLoginId(userLoginId);
    }

}
