package com.lumie.tuition.application.service;

import com.lumie.tuition.application.dto.response.GuardianResponse;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianQueryService {

    private final GuardianRepository guardianRepository;

    public GuardianResponse getById(Long id) {
        return guardianRepository.findById(id)
                .map(GuardianResponse::from)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.GUARDIAN_NOT_FOUND,
                        "Guardian not found: " + id));
    }

    public Page<GuardianResponse> listAll(Pageable pageable) {
        return guardianRepository.findAll(pageable)
                .map(GuardianResponse::from);
    }
}
