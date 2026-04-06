package com.lumie.notification.application.service;

import com.lumie.notification.application.dto.*;
import com.lumie.notification.application.port.out.AcademyServicePort;
import com.lumie.notification.application.port.out.AdminServicePort;
import com.lumie.notification.application.port.out.ClassServicePort;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.exception.SmsMessageNotFoundException;
import com.lumie.notification.domain.exception.SmsTemplateNotFoundException;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.repository.SmsTemplateRepository;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SmsCommandService {

    private final SmsMessageRepository messageRepository;
    private final SmsTemplateRepository templateRepository;
    private final AcademyServicePort academyServicePort;
    private final AdminServicePort adminServicePort;
    private final ClassServicePort classServicePort;

    public SmsMessageResponse sendSms(SendSmsRequest request) {
        academyServicePort.getAcademy(request.academyId())
            .filter(AcademyServicePort.AcademyInfo::isActive)
            .orElseThrow(() -> new BusinessException(SmsErrorCode.ACADEMY_NOT_FOUND,
                "Academy not found or inactive: " + request.academyId()));

        adminServicePort.getAdmin(request.senderId())
            .orElseThrow(() -> new BusinessException(SmsErrorCode.ACADEMY_NOT_FOUND,
                "Sender not found with id: " + request.senderId()));

        if (request.recipients() == null || request.recipients().isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "Recipients list cannot be empty");
        }

        SmsMessage message = SmsMessage.create(
            request.academyId(),
            request.senderId(),
            request.recipientType(),
            request.recipients(),
            request.title(),
            request.content(),
            request.templateId()
        );

        // Mock send: mark as SENT immediately
        message.markAsSent();

        SmsMessage saved = messageRepository.save(message);
        log.info("SMS message sent (mock): {} to {} recipients in academy: {}",
            saved.getId(), saved.getRecipients().size(), saved.getAcademyId());
        return SmsMessageResponse.from(saved);
    }

    public SmsMessageResponse sendToClass(Long classId, SendToClassRequest request) {
        academyServicePort.getAcademy(request.academyId())
            .filter(AcademyServicePort.AcademyInfo::isActive)
            .orElseThrow(() -> new BusinessException(SmsErrorCode.ACADEMY_NOT_FOUND,
                "Academy not found or inactive: " + request.academyId()));

        adminServicePort.getAdmin(request.senderId())
            .orElseThrow(() -> new BusinessException(SmsErrorCode.ACADEMY_NOT_FOUND,
                "Sender not found with id: " + request.senderId()));

        ClassServicePort.ClassInfo classInfo = classServicePort.getClass(classId)
            .orElseThrow(() -> new BusinessException(SmsErrorCode.ACADEMY_NOT_FOUND,
                "Class not found with id: " + classId));

        List<Long> studentIds = classServicePort.getStudentIdsByClassId(classId);
        if (studentIds.isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "No students enrolled in class: " + classId);
        }

        Map<Long, AcademyServicePort.StudentInfo> students = academyServicePort.getStudentsByIds(studentIds);

        List<Recipient> recipients = students.values().stream()
            .map(s -> new Recipient(s.phone(), s.name(), s.id()))
            .toList();

        if (recipients.isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "No valid recipients found in class: " + classId);
        }

        SmsMessage message = SmsMessage.create(
            request.academyId(),
            request.senderId(),
            RecipientType.STUDENT,
            recipients,
            request.title(),
            request.content(),
            request.templateId()
        );

        // Mock send
        message.markAsSent();

        SmsMessage saved = messageRepository.save(message);
        log.info("SMS message sent to class {} (mock): {} to {} recipients",
            classId, saved.getId(), saved.getRecipients().size());
        return SmsMessageResponse.from(saved);
    }

    public SmsTemplateResponse createTemplate(CreateTemplateRequest request) {
        academyServicePort.getAcademy(request.academyId())
            .filter(AcademyServicePort.AcademyInfo::isActive)
            .orElseThrow(() -> new BusinessException(SmsErrorCode.ACADEMY_NOT_FOUND,
                "Academy not found or inactive: " + request.academyId()));

        SmsTemplate template = SmsTemplate.create(
            request.academyId(),
            request.name(),
            request.content(),
            request.category()
        );

        SmsTemplate saved = templateRepository.save(template);
        log.info("SMS template created: {} in academy: {}", saved.getId(), saved.getAcademyId());
        return SmsTemplateResponse.from(saved);
    }

    public SmsTemplateResponse updateTemplate(Long id, UpdateTemplateRequest request) {
        SmsTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new SmsTemplateNotFoundException(id));

        String name = request.name() != null ? request.name() : template.getName();
        String content = request.content() != null ? request.content() : template.getContent();
        var category = request.category() != null ? request.category() : template.getCategory();

        template.updateInfo(name, content, category);

        SmsTemplate saved = templateRepository.save(template);
        log.info("SMS template updated: {}", saved.getId());
        return SmsTemplateResponse.from(saved);
    }

    public void deleteTemplate(Long id) {
        templateRepository.findById(id)
            .orElseThrow(() -> new SmsTemplateNotFoundException(id));

        templateRepository.deleteById(id);
        log.info("SMS template deleted: {}", id);
    }
}
