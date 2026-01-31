package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.AcademyResponse;
import com.lumie.academy.domain.entity.Academy;
import com.lumie.academy.domain.exception.AcademyNotFoundException;
import com.lumie.academy.domain.repository.AcademyRepository;
import com.lumie.academy.domain.repository.StudentRepository;
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
public class AcademyQueryService {

    private final AcademyRepository academyRepository;
    private final StudentRepository studentRepository;

    public AcademyResponse getAcademy(Long id) {
        Academy academy = academyRepository.findById(id)
                .orElseThrow(() -> new AcademyNotFoundException(id));
        long studentCount = studentRepository.countByAcademyId(id);
        return AcademyResponse.from(academy, studentCount);
    }

    public AcademyResponse getAcademyByName(String name) {
        Academy academy = academyRepository.findByName(name)
                .orElseThrow(() -> new AcademyNotFoundException(name));
        long studentCount = studentRepository.countByAcademyId(academy.getId());
        return AcademyResponse.from(academy, studentCount);
    }

    public AcademyResponse getDefaultAcademy() {
        Academy academy = academyRepository.findDefaultAcademy()
                .orElseThrow(() -> new AcademyNotFoundException("default"));
        long studentCount = studentRepository.countByAcademyId(academy.getId());
        return AcademyResponse.from(academy, studentCount);
    }

    public Page<AcademyResponse> getAllAcademies(Pageable pageable) {
        return academyRepository.findAll(pageable)
                .map(academy -> {
                    long studentCount = studentRepository.countByAcademyId(academy.getId());
                    return AcademyResponse.from(academy, studentCount);
                });
    }

    public Page<AcademyResponse> getActiveAcademies(Pageable pageable) {
        return academyRepository.findByStatus("ACTIVE", pageable)
                .map(academy -> {
                    long studentCount = studentRepository.countByAcademyId(academy.getId());
                    return AcademyResponse.from(academy, studentCount);
                });
    }

    public long countAcademies() {
        return academyRepository.count();
    }

    public long countActiveAcademies() {
        return academyRepository.countByStatus("ACTIVE");
    }
}
