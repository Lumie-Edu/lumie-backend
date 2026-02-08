package com.lumie.admin.application.service;

import com.lumie.admin.adapter.out.persistence.AdminSpecification;
import com.lumie.admin.application.dto.AdminFilterRequest;
import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQueryService {

    private final AdminRepository adminRepository;

    public AdminResponse getAdmin(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));
        return AdminResponse.from(admin);
    }

    public AdminResponse getAdminByUserLoginId(String userLoginId) {
        Admin admin = adminRepository.findByUserLoginId(userLoginId)
                .orElseThrow(() -> new AdminNotFoundException(userLoginId));
        return AdminResponse.from(admin);
    }

    public Page<AdminResponse> getAllAdmins(AdminFilterRequest filter, Pageable pageable) {
        if (filter != null && hasAnyFilter(filter)) {
            return adminRepository.findAll(AdminSpecification.withFilters(filter), pageable)
                    .map(AdminResponse::from);
        }
        return adminRepository.findAll(pageable)
                .map(AdminResponse::from);
    }

    public boolean existsByUserLoginId(String userLoginId) {
        return adminRepository.existsByUserLoginId(userLoginId);
    }

    private boolean hasAnyFilter(AdminFilterRequest filter) {
        return filter.positionId() != null || filter.academyId() != null
                || filter.isActive() != null || (filter.search() != null && !filter.search().isBlank());
    }
}
