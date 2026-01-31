package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.AdminRequest;
import com.lumie.academy.application.dto.AdminResponse;
import com.lumie.academy.application.port.out.BillingServicePort;
import com.lumie.academy.application.port.out.MemberEventPublisherPort;
import com.lumie.academy.domain.entity.Academy;
import com.lumie.academy.domain.entity.Admin;
import com.lumie.academy.domain.exception.AcademyNotFoundException;
import com.lumie.academy.domain.exception.AdminNotFoundException;
import com.lumie.academy.domain.exception.DuplicateEmailException;
import com.lumie.academy.domain.exception.QuotaExceededException;
import com.lumie.academy.domain.repository.AdminRepository;
import com.lumie.academy.domain.repository.AcademyRepository;
import com.lumie.academy.infrastructure.tenant.TenantContextHolder;
import com.lumie.messaging.event.MemberCreatedEvent;
import com.lumie.messaging.event.MemberUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCommandService {

    private final AdminRepository adminRepository;
    private final AcademyRepository academyRepository;
    private final PasswordEncoder passwordEncoder;
    private final BillingServicePort billingServicePort;
    private final MemberEventPublisherPort eventPublisher;

    public AdminResponse registerAdmin(AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        checkAdminQuota(tenantSlug);

        if (adminRepository.existsByUserEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Academy academy = null;
        if (request.academyId() != null) {
            academy = academyRepository.findById(request.academyId())
                    .orElseThrow(() -> new AcademyNotFoundException(request.academyId()));
        }

        String passwordHash = passwordEncoder.encode(request.password());

        Admin admin = Admin.create(
            request.email(),
            passwordHash,
            request.name(),
            request.phone(),
            academy,
            request.adminType()
        );

        Admin saved = adminRepository.save(admin);

        eventPublisher.publish(new MemberCreatedEvent(
            saved.getId(),
            tenantSlug,
            "ADMIN",
            saved.getAdminName(),
            saved.getAdminEmail(),
            academy != null ? academy.getId() : null
        ));

        log.info("Admin registered: {} in tenant: {}", saved.getAdminEmail(), tenantSlug);
        return AdminResponse.from(saved);
    }

    public AdminResponse updateAdmin(Long id, AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        admin.updateInfo(request.name(), request.phone(), request.adminType());

        if (request.academyId() != null) {
            Academy academy = academyRepository.findById(request.academyId())
                    .orElseThrow(() -> new AcademyNotFoundException(request.academyId()));
            admin.assignToAcademy(academy);
        }

        Admin updated = adminRepository.save(admin);

        eventPublisher.publish(new MemberUpdatedEvent(
            updated.getId(),
            tenantSlug,
            "ADMIN",
            updated.getAdminName()
        ));

        log.info("Admin updated: {} in tenant: {}", id, tenantSlug);
        return AdminResponse.from(updated);
    }

    public void deactivateAdmin(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        admin.deactivate();
        adminRepository.save(admin);

        log.info("Admin deactivated: {} in tenant: {}", id, tenantSlug);
    }

    private void checkAdminQuota(String tenantSlug) {
        var result = billingServicePort.checkQuota(tenantSlug, "MAX_ADMINS");
        if (!result.allowed()) {
            throw new QuotaExceededException("MAX_ADMINS", result.currentUsage(), result.limit());
        }
    }
}
