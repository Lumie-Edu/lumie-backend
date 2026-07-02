package com.lumie.student.adapter.in.event;

import com.lumie.auth.api.StudentSelfRegisteredEvent;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.student.domain.entity.Student;
import com.lumie.student.domain.exception.StudentErrorCode;
import com.lumie.student.domain.repository.StudentRepository;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final StudentRepository studentRepository;

    @EventListener
    @Order(1) // Ensure it runs right after the event is published synchronously in the same transaction
    public void onStudentSelfRegistered(StudentSelfRegisteredEvent event) {
        log.info("Received StudentSelfRegisteredEvent for user ID: {} in tenant: {}", event.userId(), event.tenantSlug());

        TenantContextHolder.withinContext(event.tenantSlug(), event.tenantId(), () -> {
            if (studentRepository.findByUserId(event.userId()).isPresent()) {
                log.info("Student entity already exists for user ID: {}", event.userId());
                return null;
            }

            validatePhoneUniqueness(event.phone());

            Student student = Student.create(
                    event.userId(),
                    event.userLoginId(),
                    event.name(),
                    event.phone(),
                    event.studentHighschool(),
                    event.studentBirthYear(),
                    event.parentPhone(),
                    null // studentMemo
            );
            studentRepository.save(student);
            log.info("Student entity created for user ID: {}", event.userId());
            return null;
        });
    }

    private void validatePhoneUniqueness(String phone) {
        String normalizedPhone = PhoneNumberUtils.toDigits(phone);

        if (normalizedPhone != null && studentRepository.existsByPhone(normalizedPhone)) {
            throw new BusinessException(StudentErrorCode.DUPLICATE_PHONE);
        }
    }
}
