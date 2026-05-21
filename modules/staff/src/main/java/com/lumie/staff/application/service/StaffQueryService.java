package com.lumie.staff.application.service;

import com.lumie.staff.adapter.out.persistence.StaffSpecification;
import com.lumie.staff.application.dto.request.StaffFilterRequest;
import com.lumie.staff.application.dto.response.StaffPermissionResponse;
import com.lumie.staff.domain.vo.StaffFilter;
import com.lumie.staff.application.dto.response.StaffResponse;
import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.exception.StaffNotFoundException;
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
                .orElseThrow(() -> new StaffNotFoundException(id));
        return enrich(staff);
    }

    public StaffResponse getStaffByUserLoginId(String userLoginId) {
        Staff staff = staffRepository.findByUserLoginId(userLoginId)
                .orElseThrow(() -> new StaffNotFoundException(userLoginId));
        return enrich(staff);
    }

    public Page<StaffResponse> getAllStaff(StaffFilterRequest filter, Pageable pageable) {
        Page<Staff> page = (filter != null && hasAnyFilter(filter))
                ? staffRepository.findAll(StaffSpecification.withFilters(new StaffFilter(filter.isActive(), filter.search())), pageable)
                : staffRepository.findAll(pageable);
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

    @Cacheable(value = CacheConfig.CACHE_ADMIN_PERMISSIONS, key = "#staffId")
    public List<StaffPermissionResponse> getStaffPermissions(Long staffId) {
        staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        return staffPermissionRepository.findByStaffId(staffId).stream()
                .map(StaffPermissionResponse::from)
                .toList();
    }

    public boolean existsByUserLoginId(String userLoginId) {
        return staffRepository.existsByUserLoginId(userLoginId);
    }

    private boolean hasAnyFilter(StaffFilterRequest filter) {
        return filter.isActive() != null || (filter.search() != null && !filter.search().isBlank());
    }
}
