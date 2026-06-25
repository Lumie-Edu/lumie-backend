package com.lumie.notification.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.auth.Role;
import com.lumie.staff.api.StaffService;
import com.lumie.classroom.api.ClassService;
import com.lumie.student.api.StudentService;
import com.lumie.notification.application.dto.request.RecipientRequest;
import com.lumie.notification.application.dto.request.SendSmsRequest;
import com.lumie.notification.application.dto.request.SendToClassRequest;
import com.lumie.notification.application.dto.request.CreateTemplateRequest;
import com.lumie.notification.application.dto.request.UpdateTemplateRequest;
import com.lumie.notification.application.port.out.SmsRecipientGroupPersistencePort;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.dto.response.SmsTemplateResponse;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.repository.SmsTemplateRepository;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsMessageType;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

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
    private final SmsRecipientGroupPersistencePort recipientGroupPersistencePort;
    private final Clock clock = Clock.systemUTC();

    public SmsMessageResponse sendSms(SendSmsRequest request) {
        String slug = TenantContextHolder.getRequiredTenant();
        Long senderId = resolveSenderId(slug);
        boolean scheduled = validateScheduledAt(request.scheduledAt());
        SendTarget sendTarget = resolveSendTarget(request);

        List<Recipient> recipients = resolveRecipients(request, senderId, sendTarget.groupSend()).stream()
            .map(recipient -> renderRecipient(recipient, request.content(), null))
            .toList();
        validateRecipients(recipients);
        MessageEstimate estimate = estimate(recipients, request.content());

        SmsMessage message = SmsMessage.create(
            senderId,
            sendTarget.recipientType(),
            recipients,
            request.title(),
            sendTarget.recipientGroupId(),
            request.content(),
            request.templateId(),
            request.scheduledAt(),
            estimate.messageType(),
            estimate.segments(),
            estimate.unitPrice(),
            estimate.totalPrice()
        );

        if (!scheduled) {
            applyMockDispatch(message, recipients);
        }

        SmsMessage saved = messageRepository.save(message);
        log.info("SMS message queued: {} to {} recipients status={}",
            saved.getId(), saved.getRecipients().size(), saved.getStatus());
        return SmsMessageResponse.from(saved);
    }

    public SmsMessageResponse sendToClass(Long classId, SendToClassRequest request) {
        String slug = TenantContextHolder.getRequiredTenant();
        Long senderId = resolveSenderId(slug);
        boolean scheduled = validateScheduledAt(request.scheduledAt());

        ClassService.ClassData classData = classService.getClass(slug, classId)
            .orElseThrow(() -> new BusinessException(SmsErrorCode.CLASS_NOT_FOUND,
                "Class not found with id: " + classId));

        List<Long> studentIds = classService.getEnrolledStudentIds(slug, classId).stream()
            .filter(Objects::nonNull)
            .toList();
        if (studentIds.isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "No students enrolled in class: " + classId);
        }

        List<StudentService.StudentData> students =
                studentService.getStudentsByIds(slug, studentIds).students();

        List<Recipient> recipients = students.stream()
            .map(s -> Recipient.pending(s.parentPhone(), s.studentName(), s.id(), classData.name(), null))
            .map(recipient -> renderRecipient(recipient, request.content(), classData.name()))
            .toList();

        if (recipients.isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "No valid recipients found in class: " + classId);
        }
        validateRecipients(recipients);
        MessageEstimate estimate = estimate(recipients, request.content());

        SmsMessage message = SmsMessage.create(
            senderId,
            RecipientType.GUARDIAN,
            recipients,
            request.title(),
            request.content(),
            request.templateId(),
            request.scheduledAt(),
            estimate.messageType(),
            estimate.segments(),
            estimate.unitPrice(),
            estimate.totalPrice()
        );

        if (!scheduled) {
            applyMockDispatch(message, recipients);
        }

        SmsMessage saved = messageRepository.save(message);
        log.info("SMS message queued to class {}: {} to {} recipients status={}",
            classId, saved.getId(), saved.getRecipients().size(), saved.getStatus());
        return SmsMessageResponse.from(saved);
    }

    public SmsMessageResponse cancelScheduled(Long id) {
        String slug = TenantContextHolder.getRequiredTenant();
        Long senderId = resolveSenderId(slug);
        Role role = UserContextHolder.getUserRole();
        boolean elevated = role == Role.OWNER || role == Role.MANAGER;
        SmsMessage message = (elevated
                ? messageRepository.findById(id)
                : messageRepository.findByIdAndSenderId(id, senderId))
            .orElseThrow(() -> new BusinessException(SmsErrorCode.MESSAGE_NOT_FOUND, "SMS message not found with id: %d".formatted(id)));
        validateCancelAuthority(message, senderId);
        if (message.getStatus() == com.lumie.notification.domain.vo.SmsStatus.CANCELLED) {
            return SmsMessageResponse.from(message);
        }
        if (!message.isPending() || message.getScheduledAt() == null) {
            throw new BusinessException(SmsErrorCode.MESSAGE_NOT_PENDING, "Only pending scheduled SMS messages can be cancelled");
        }
        message.cancel();
        return SmsMessageResponse.from(messageRepository.save(message));
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
        SmsTemplate ignored = templateRepository.findById(id)
            .orElseThrow(() -> new BusinessException(SmsErrorCode.TEMPLATE_NOT_FOUND, "SMS template not found with id: %d".formatted(id)));

        templateRepository.deleteById(id);
        log.info("SMS template deleted: {}", id);
    }

    private List<Recipient> resolveRecipients(SendSmsRequest request, Long senderId, boolean groupSend) {
        if (groupSend) {
            return recipientGroupPersistencePort.findByIdAndOwnerStaffId(request.recipientGroupId(), senderId)
                    .map(group -> List.copyOf(group.getRecipients()))
                    .orElseThrow(() -> new BusinessException(
                            SmsErrorCode.RECIPIENT_GROUP_NOT_FOUND,
                            "SMS recipient group not found with id: %d".formatted(request.recipientGroupId())
                    ));
        }

        if (request.recipients() == null || request.recipients().isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "Recipients list cannot be empty");
        }

        return request.recipients().stream()
                .map(RecipientRequest::toVo)
                .toList();
    }

    private SendTarget resolveSendTarget(SendSmsRequest request) {
        if (request.recipientGroupId() != null) {
            if (request.recipients() != null) {
                throw new BusinessException(
                        SmsErrorCode.INVALID_SEND_TARGET,
                        "Group SMS requests must use recipientGroupId without inline recipients"
                );
            }
            if (request.recipientType() != RecipientType.GUARDIAN) {
                throw new BusinessException(
                        SmsErrorCode.INVALID_RECIPIENT_GROUP_TYPE,
                        "Recipient groups currently support guardian recipients only"
                );
            }
            return new SendTarget(true, request.recipientGroupId(), RecipientType.GUARDIAN);
        }

        if (request.recipients() == null || request.recipients().isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "Recipients list cannot be empty");
        }
        return new SendTarget(false, null, request.recipientType());
    }

    private Long resolveSenderId(String slug) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(SmsErrorCode.SENDER_NOT_FOUND, "Current user is not authenticated");
        }
        return staffService.getStaffByUserId(slug, userId)
                .map(StaffService.StaffData::id)
                .orElseThrow(() -> new BusinessException(SmsErrorCode.SENDER_NOT_FOUND,
                        "Staff sender not found for current user"));
    }

    private void validateCancelAuthority(SmsMessage message, Long senderId) {
        Role role = UserContextHolder.getUserRole();
        boolean elevated = role == Role.OWNER || role == Role.MANAGER;
        if (!elevated && !Objects.equals(message.getSenderId(), senderId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Cannot cancel another staff member's scheduled SMS");
        }
    }

    private Recipient renderRecipient(Recipient recipient, String template, String className) {
        String rendered = template
                .replace("{학생명}", nullToEmpty(recipient.name()))
                .replace("{반이름}", nullToEmpty(recipient.className() != null ? recipient.className() : className))
                .replace("{날짜}", java.time.LocalDate.now(clock).toString());
        return Recipient.pending(
                PhoneNumberUtils.toDigits(recipient.phone()),
                recipient.name(),
                recipient.studentId(),
                recipient.className() != null ? recipient.className() : className,
                rendered
        );
    }

    private void applyMockDispatch(SmsMessage message, List<Recipient> recipients) {
        List<Recipient> dispatched = recipients.stream()
                .map(recipient -> PhoneNumberUtils.isValid(recipient.phone())
                        ? recipient.sent()
                        : recipient.failed("INVALID_PHONE"))
                .toList();
        int successCount = (int) dispatched.stream()
                .filter(recipient -> recipient.deliveryStatus() == com.lumie.notification.domain.vo.SmsStatus.SENT)
                .count();
        int failCount = dispatched.size() - successCount;
        if (failCount == 0) {
            message.markAsSent(dispatched);
        } else if (successCount == 0) {
            message.markAsFailed(dispatched);
        } else {
            message.markAsPartial(dispatched, successCount, failCount);
        }
    }

    private boolean validateScheduledAt(Instant scheduledAt) {
        if (scheduledAt == null) {
            return false;
        }
        if (!scheduledAt.isAfter(Instant.now(clock))) {
            throw new BusinessException(SmsErrorCode.INVALID_SCHEDULED_AT, "Scheduled time must be in the future");
        }
        return true;
    }

    private void validateRecipients(List<Recipient> recipients) {
        if (recipients.isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "Recipients list cannot be empty");
        }
        boolean hasInvalidPhone = recipients.stream()
                .anyMatch(recipient -> !PhoneNumberUtils.isValid(recipient.phone()));
        if (hasInvalidPhone) {
            throw new BusinessException(SmsErrorCode.INVALID_RECIPIENTS, "Recipients contain invalid phone numbers");
        }
    }

    private MessageEstimate estimate(List<Recipient> recipients, String fallbackContent) {
        int maxLength = recipients.stream()
                .map(Recipient::renderedContent)
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .max()
                .orElse(fallbackContent.length());
        SmsMessageType type = maxLength > 90 ? SmsMessageType.LMS : SmsMessageType.SMS;
        int segments = type == SmsMessageType.SMS ? 1 : Math.max(1, (int) Math.ceil(maxLength / 1000.0));
        int unitPrice = type == SmsMessageType.SMS ? 20 : 50;
        return new MessageEstimate(type, segments, unitPrice, recipients.size() * segments * unitPrice);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record MessageEstimate(SmsMessageType messageType, int segments, int unitPrice, int totalPrice) {
    }

    private record SendTarget(boolean groupSend, Long recipientGroupId, RecipientType recipientType) {
    }
}
