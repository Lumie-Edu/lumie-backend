package com.lumie.staff.application.service;

import com.lumie.staff.application.dto.request.StaffFilterRequest;
import com.lumie.staff.application.dto.response.StaffPermissionResponse;
import com.lumie.staff.application.dto.response.StaffResponse;
import com.lumie.staff.domain.entity.Staff;
import com.lumie.common.exception.BusinessException;
import com.lumie.staff.domain.exception.StaffErrorCode;
import com.lumie.staff.application.port.out.StaffPermissionPersistencePort;
import com.lumie.staff.application.port.out.StaffPersistencePort;
import com.lumie.auth.api.AuthService;
import com.lumie.common.cache.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffQueryService {

    private final StaffPersistencePort staffRepository;
    private final StaffPermissionPersistencePort staffPermissionRepository;
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
        var tenantOwner = authService.getTenantOwner();
        var visibleOwner = tenantOwner
                .filter(owner -> filter == null || filter.isActive() == null || filter.isActive())
                .filter(owner -> matchesSearch(owner, filter == null ? null : filter.search()));
        boolean ownerVisible = visibleOwner.isPresent();
        Pageable staffPageable = pageable;
        if (ownerVisible) {
            long employeeOffset = Math.max(0, pageable.getOffset() - 1);
            int employeePageSize = pageable.getPageNumber() == 0
                    ? Math.max(1, pageable.getPageSize() - 1)
                    : pageable.getPageSize();
            staffPageable = new RosterPageable(
                    pageable.getPageNumber(), employeePageSize, pageable.getSort(), employeeOffset);
        }
        Page<Staff> page = staffRepository.searchExcludingUserId(
                filter == null ? null : filter.isActive(),
                filter == null ? null : filter.search(),
                tenantOwner.map(AuthService.TenantOwnerData::userId).orElse(null),
                staffPageable);
        Map<Long, String> seeds = avatarSeedsFor(page.getContent());
        List<StaffResponse> responses = page.getContent().stream()
                .map(s -> StaffResponse.from(s, seeds.get(s.getUserId())))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (ownerVisible && pageable.getPageNumber() == 0) {
            if (pageable.getPageSize() == 1) responses.clear();
            var owner = visibleOwner.orElseThrow();
            Map<Long, String> ownerSeed = authService.getAvatarSeeds(List.of(owner.userId()));
            responses.add(0, new StaffResponse(-owner.userId(), owner.userId(), owner.userLoginId(), owner.name(),
                    owner.phone(), owner.email(), "OWNER", null, true,
                    com.lumie.staff.domain.vo.EmploymentStatus.ACTIVE, ownerSeed.get(owner.userId()),
                    owner.createdAt(), owner.updatedAt()));
        }
        long total = page.getTotalElements() + (ownerVisible ? 1 : 0);
        return new PageImpl<>(responses, pageable, total);
    }

    private boolean matchesSearch(com.lumie.auth.api.AuthService.TenantOwnerData owner, String search) {
        if (search == null || search.isBlank()) return true;
        String query = search.toLowerCase(java.util.Locale.ROOT);
        return (owner.name() != null && owner.name().toLowerCase(java.util.Locale.ROOT).contains(query))
                || (owner.userLoginId() != null && owner.userLoginId().toLowerCase(java.util.Locale.ROOT).contains(query))
                || (owner.email() != null && owner.email().toLowerCase(java.util.Locale.ROOT).contains(query));
    }

    private record RosterPageable(int pageNumber, int pageSize, Sort sort, long offset) implements Pageable {
        @Override public boolean isPaged() { return true; }
        @Override public boolean isUnpaged() { return false; }
        @Override public int getPageNumber() { return pageNumber; }
        @Override public int getPageSize() { return pageSize; }
        @Override public long getOffset() { return offset; }
        @Override public Sort getSort() { return sort; }
        @Override public Pageable next() { return new RosterPageable(pageNumber + 1, pageSize, sort, offset + pageSize); }
        @Override public Pageable previousOrFirst() { return hasPrevious() ? new RosterPageable(pageNumber - 1, pageSize, sort, Math.max(0, offset - pageSize)) : first(); }
        @Override public Pageable first() { return new RosterPageable(0, pageSize, sort, 0); }
        @Override public Pageable withPage(int nextPage) {
            return new RosterPageable(nextPage, pageSize, sort, (long) nextPage * pageSize);
        }
        @Override public boolean hasPrevious() { return pageNumber > 0; }
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
