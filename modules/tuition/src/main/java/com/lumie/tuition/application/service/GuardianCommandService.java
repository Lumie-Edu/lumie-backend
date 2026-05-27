package com.lumie.tuition.application.service;

import com.lumie.tuition.application.dto.request.CreateGuardianRequest;
import com.lumie.tuition.application.dto.request.UpdateGuardianRequest;
import com.lumie.tuition.application.dto.response.GuardianResponse;
import com.lumie.tuition.domain.entity.Guardian;
import com.lumie.tuition.domain.entity.StudentGuardian;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.GuardianRepository;
import com.lumie.tuition.domain.repository.StudentGuardianRepository;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GuardianCommandService {

    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    public GuardianResponse createGuardian(CreateGuardianRequest request) {
        Guardian guardian = Guardian.create(
                request.name(),
                request.phone(),
                request.email(),
                request.relationship()
        );
        Guardian saved = guardianRepository.save(guardian);
        log.info("Guardian created: id={}", saved.getId());
        return GuardianResponse.from(saved);
    }

    public GuardianResponse updateGuardian(Long id, UpdateGuardianRequest request) {
        Guardian guardian = guardianRepository.findById(id)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.GUARDIAN_NOT_FOUND,
                        "Guardian not found: " + id));
        guardian.updateInfo(request.name(), request.phone(), request.email(), request.relationship());
        Guardian saved = guardianRepository.save(guardian);
        log.info("Guardian updated: id={}", id);
        return GuardianResponse.from(saved);
    }

    public void deleteGuardian(Long id) {
        Guardian guardian = guardianRepository.findById(id)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.GUARDIAN_NOT_FOUND,
                        "Guardian not found: " + id));
        guardianRepository.delete(guardian);
        log.info("Guardian deleted: id={}", id);
    }

    public void linkGuardianToStudent(Long studentId, Long guardianId, boolean isPrimary) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (studentGuardianRepository.existsByStudentIdAndGuardianId(studentId, guardianId)) {
            throw new TuitionException(TuitionErrorCode.GUARDIAN_ALREADY_LINKED,
                    "Guardian %d is already linked to student %d".formatted(guardianId, studentId));
        }
        StudentGuardian link = StudentGuardian.create(studentId, guardianId, tenantId, isPrimary);
        studentGuardianRepository.save(link);
        log.info("Guardian {} linked to student {}", guardianId, studentId);
    }

    public void unlinkGuardianFromStudent(Long studentId, Long guardianId) {
        studentGuardianRepository.deleteByStudentIdAndGuardianId(studentId, guardianId);
        log.info("Guardian {} unlinked from student {}", guardianId, studentId);
    }
}
