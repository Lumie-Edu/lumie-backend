package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.request.AdminFilterRequest;
import com.lumie.admin.application.dto.response.AdminPermissionResponse;
import com.lumie.admin.domain.vo.AdminFilter;
import com.lumie.admin.application.dto.response.AdminResponse;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.repository.AdminPermissionRepository;
import com.lumie.admin.domain.repository.AdminRepository;
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
public class AdminQueryService {

    private final AdminRepository adminRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final AuthService authService;

    public AdminResponse getAdmin(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        return enrich(admin);
    }

    public AdminResponse getAdminByUserLoginId(String userLoginId) {
        Admin admin = adminRepository.findByUserLoginId(userLoginId)
                .orElseThrow(() -> new AdminNotFoundException(userLoginId));
        return enrich(admin);
    }

    public Page<AdminResponse> getAllAdmins(AdminFilterRequest filter, Pageable pageable) {
        Page<Admin> page = (filter != null && hasAnyFilter(filter))
                ? adminRepository.findAllByFilter(new AdminFilter(filter.isActive(), filter.search()), pageable)
                : adminRepository.findAll(pageable);
        Map<Long, String> seeds = avatarSeedsFor(page.getContent());
        return page.map(a -> AdminResponse.from(a, seeds.get(a.getUserId())));
    }

    private AdminResponse enrich(Admin admin) {
        Map<Long, String> seeds = authService.getAvatarSeeds(List.of(admin.getUserId()));
        return AdminResponse.from(admin, seeds.get(admin.getUserId()));
    }

    private Map<Long, String> avatarSeedsFor(List<Admin> admins) {
        if (admins.isEmpty()) return Map.of();
        return authService.getAvatarSeeds(admins.stream().map(Admin::getUserId).toList());
    }

    @Cacheable(value = CacheConfig.CACHE_ADMIN_PERMISSIONS, key = "#adminId")
    public List<AdminPermissionResponse> getAdminPermissions(Long adminId) {
        adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException(adminId));
        return adminPermissionRepository.findByAdminId(adminId).stream()
                .map(AdminPermissionResponse::from)
                .toList();
    }

    public boolean existsByUserLoginId(String userLoginId) {
        return adminRepository.existsByUserLoginId(userLoginId);
    }

    private boolean hasAnyFilter(AdminFilterRequest filter) {
        return filter.isActive() != null || (filter.search() != null && !filter.search().isBlank());
    }
}
