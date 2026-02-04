package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.AdminRequest;
import com.lumie.academy.application.dto.AdminResponse;
import com.lumie.academy.application.port.out.BillingServicePort;
import com.lumie.academy.application.port.out.MemberEventPublisherPort;
import com.lumie.academy.domain.entity.Academy;
import com.lumie.academy.domain.entity.Admin;
import com.lumie.academy.domain.exception.AcademyNotFoundException;
import com.lumie.academy.domain.exception.AdminNotFoundException;
import com.lumie.academy.domain.exception.DuplicateUserLoginIdException;
import com.lumie.academy.domain.exception.QuotaExceededException;
import com.lumie.academy.domain.repository.AdminRepository;
import com.lumie.academy.domain.repository.AcademyRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.messaging.event.MemberCreatedEvent;
import com.lumie.messaging.event.MemberUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCommandService {

    private final AdminRepository adminRepository;
    private final AcademyRepository academyRepository;
    private final BillingServicePort billingServicePort;
    private final MemberEventPublisherPort eventPublisher;

    public AdminResponse registerAdmin(Long userId, AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        checkAdminQuota(tenantSlug);

        if (adminRepository.existsByUserLoginId(request.userLoginId())) {
            throw new DuplicateUserLoginIdException(request.userLoginId());
        }

        Set<Academy> academies = new HashSet<>();
        if (request.academyIds() != null && !request.academyIds().isEmpty()) {
            for (Long academyId : request.academyIds()) {
                Academy academy = academyRepository.findById(academyId)
                        .orElseThrow(() -> new AcademyNotFoundException(academyId));
                academies.add(academy);
            }
        }

        Admin admin = Admin.create(
            userId,
            request.userLoginId(),
            request.name(),
            request.phone(),
            academies,
            request.adminPosition(),
            request.adminMemo()
        );

        Admin saved = adminRepository.save(admin);

        Long firstAcademyId = academies.isEmpty() ? null : academies.iterator().next().getId();
        eventPublisher.publish(new MemberCreatedEvent(
            saved.getId(),
            tenantSlug,
            "ADMIN",
            saved.getAdminName(),
            saved.getUserLoginId(),
            firstAcademyId
        ));

        log.info("Admin registered: {} in tenant: {}", saved.getUserLoginId(), tenantSlug);
        return AdminResponse.from(saved);
    }

    public AdminResponse updateAdmin(Long id, AdminRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException(id));

        admin.updateInfo(request.name(), request.phone(), request.adminPosition(), request.adminMemo());

        if (request.academyIds() != null) {
            Set<Academy> academies = new HashSet<>();
            for (Long academyId : request.academyIds()) {
                Academy academy = academyRepository.findById(academyId)
                        .orElseThrow(() -> new AcademyNotFoundException(academyId));
                academies.add(academy);
            }
            admin.setAcademies(academies);
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
