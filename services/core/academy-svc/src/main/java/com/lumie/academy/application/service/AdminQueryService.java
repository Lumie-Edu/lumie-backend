package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.AdminResponse;
import com.lumie.academy.domain.entity.Admin;
import com.lumie.academy.domain.exception.AdminNotFoundException;
import com.lumie.academy.domain.repository.AdminRepository;
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

    public AdminResponse getAdminByEmail(String email) {
        Admin admin = adminRepository.findByUserEmail(email)
                .orElseThrow(() -> new AdminNotFoundException(email));
        return AdminResponse.from(admin);
    }

    public Page<AdminResponse> getAdminsByAcademy(Long academyId, Pageable pageable) {
        return adminRepository.findByAcademyId(academyId, pageable)
                .map(AdminResponse::from);
    }

    public Page<AdminResponse> getAllAdmins(Pageable pageable) {
        return adminRepository.findAll(pageable)
                .map(AdminResponse::from);
    }

    public long countAdminsByAcademy(Long academyId) {
        return adminRepository.countByAcademyId(academyId);
    }

    public boolean existsByEmail(String email) {
        return adminRepository.existsByUserEmail(email);
    }
}
