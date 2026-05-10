package com.lumie.exam.adapter.in.internal;

import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.api.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamServiceAdapter implements ExamService {

    private final ExamResultRepository examResultRepository;

    @Override
    @Transactional
    public int linkUnregisteredResultsByPhone(String phoneNumber, Long studentId) {
        if (phoneNumber == null || phoneNumber.isBlank() || studentId == null) {
            return 0;
        }
        int updated = examResultRepository.linkUnregisteredByPhone(phoneNumber, studentId);
        if (updated > 0) {
            log.info("Linked {} unregistered exam results to studentId={} (phone={})",
                    updated, studentId, phoneNumber);
        }
        return updated;
    }
}
