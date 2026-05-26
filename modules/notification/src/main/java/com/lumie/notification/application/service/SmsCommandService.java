package com.lumie.notification.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.staff.api.StaffService;
import com.lumie.classroom.api.ClassService;
import com.lumie.student.api.StudentService;
import com.lumie.notification.application.dto.request.RecipientRequest;
import com.lumie.notification.application.dto.request.SendSmsRequest;
import com.lumie.notification.application.dto.request.SendToClassRequest;
import com.lumie.notification.application.dto.request.CreateTemplateRequest;
import com.lumie.notification.application.dto.request.UpdateTemplateRequest;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.dto.response.SmsTemplateResponse;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.notification.domain.exception.SmsErrorCode;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SmsCommandService {

    private final SmsMessageRepository messageRepository;
    private final SmsTemplateRepository templateRepository;
    private final StudentService studentService;
    private final StaffService staffService;
    private final ClassService classService;

    public SmsMessageResponse sendSms(SendSmsRequest request) {
        String slug = TenantContextHolder.getRequiredTenant();
        staffService.getStaff(slug, request.senderId())
            .orElseThrow(() -> new BusinessException(SmsErrorCode.SENDER_NOT_FOUND,
                "Sender not found with id: " + request.senderId()));

        if (request.recipients() == null || request.recipients().isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "Recipients list cannot be empty");
        }

        List<Recipient> recipients = request.recipients().stream()
            .map(RecipientRequest::toVo)
            .toList();

        SmsMessage message = SmsMessage.create(
            request.senderId(),
            request.recipientType(),
            recipients,
            request.title(),
            request.content(),
            request.templateId()
        );

        // Mock send: mark as SENT immediately
        message.markAsSent();

        SmsMessage saved = messageRepository.save(message);
        log.info("SMS message sent (mock): {} to {} recipients",
            saved.getId(), saved.getRecipients().size());
        return SmsMessageResponse.from(saved);
    }

    public SmsMessageResponse sendToClass(Long classId, SendToClassRequest request) {
        String slug = TenantContextHolder.getRequiredTenant();
        staffService.getStaff(slug, request.senderId())
            .orElseThrow(() -> new BusinessException(SmsErrorCode.SENDER_NOT_FOUND,
                "Sender not found with id: " + request.senderId()));

        classService.getClass(slug, classId)
            .orElseThrow(() -> new BusinessException(SmsErrorCode.CLASS_NOT_FOUND,
                "Class not found with id: " + classId));

        List<Long> studentIds = classService.getEnrolledStudentIds(slug, classId);
        if (studentIds.isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "No students enrolled in class: " + classId);
        }

        List<StudentService.StudentData> students =
                studentService.getStudentsByIds(slug, studentIds).students();

        List<Recipient> recipients = students.stream()
            .map(s -> new Recipient(s.studentPhone(), s.studentName(), s.id()))
            .toList();

        if (recipients.isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "No valid recipients found in class: " + classId);
        }

        SmsMessage message = SmsMessage.create(
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
        SmsTemplate template = SmsTemplate.create(
            request.name(),
            request.content(),
            request.category()
        );

        SmsTemplate saved = templateRepository.save(template);
        log.info("SMS template created: {}", saved.getId());
        return SmsTemplateResponse.from(saved);
    }

    public SmsTemplateResponse updateTemplate(Long id, UpdateTemplateRequest request) {
        SmsTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new BusinessException(SmsErrorCode.TEMPLATE_NOT_FOUND, "SMS template not found with id: %d".formatted(id)));

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
            .orElseThrow(() -> new BusinessException(SmsErrorCode.TEMPLATE_NOT_FOUND, "SMS template not found with id: %d".formatted(id)));

        templateRepository.deleteById(id);
        log.info("SMS template deleted: {}", id);
    }
}
