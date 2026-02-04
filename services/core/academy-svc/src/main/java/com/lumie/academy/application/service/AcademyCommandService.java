package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.AcademyRequest;
import com.lumie.academy.application.dto.AcademyResponse;
import com.lumie.academy.application.port.out.BillingServicePort;
import com.lumie.academy.domain.entity.Academy;
import com.lumie.academy.domain.exception.AcademyNotFoundException;
import com.lumie.academy.domain.exception.QuotaExceededException;
import com.lumie.academy.domain.repository.AcademyRepository;
import com.lumie.academy.domain.repository.StudentRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AcademyCommandService {

    private final AcademyRepository academyRepository;
    private final StudentRepository studentRepository;
    private final BillingServicePort billingServicePort;

    public AcademyResponse createAcademy(AcademyRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        checkAcademyQuota(tenantSlug);

        if (academyRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Academy", request.name());
        }

        Academy academy = Academy.create(
            request.name(),
            request.address(),
            request.phone()
        );

        Academy saved = academyRepository.save(academy);

        log.info("Academy created: {} in tenant: {}", saved.getName(), tenantSlug);
        return AcademyResponse.from(saved, 0);
    }

    public AcademyResponse updateAcademy(Long id, AcademyRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Academy academy = academyRepository.findById(id)
                .orElseThrow(() -> new AcademyNotFoundException(id));

        if (!academy.getName().equals(request.name()) && academyRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Academy", request.name());
        }

        academy.updateInfo(
            request.name(),
            request.address(),
            request.phone()
        );

        Academy updated = academyRepository.save(academy);
        long studentCount = studentRepository.countByAcademyId(id);

        log.info("Academy updated: {} in tenant: {}", id, tenantSlug);
        return AcademyResponse.from(updated, studentCount);
    }

    public void toggleAcademyActive(Long id, boolean isActive) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Academy academy = academyRepository.findById(id)
                .orElseThrow(() -> new AcademyNotFoundException(id));

        if (isActive) {
            academy.activate();
        } else {
            academy.deactivate();
        }
        academyRepository.save(academy);

        log.info("Academy {} toggled to {}: in tenant: {}", id, isActive ? "active" : "inactive", tenantSlug);
    }

    public void deleteAcademy(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Academy academy = academyRepository.findById(id)
                .orElseThrow(() -> new AcademyNotFoundException(id));

        long studentCount = studentRepository.countByAcademyId(id);
        if (studentCount > 0) {
            throw new IllegalStateException("Cannot delete academy with " + studentCount + " students");
        }

        academyRepository.delete(academy);

        log.info("Academy permanently deleted: {} in tenant: {}", id, tenantSlug);
    }

    private void checkAcademyQuota(String tenantSlug) {
        var result = billingServicePort.checkQuota(tenantSlug, "MAX_ACADEMIES");
        if (!result.allowed()) {
            throw new QuotaExceededException("MAX_ACADEMIES", result.currentUsage(), result.limit());
        }
    }
}
