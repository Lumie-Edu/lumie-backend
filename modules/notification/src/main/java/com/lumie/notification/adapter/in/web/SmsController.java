package com.lumie.notification.adapter.in.web;

import com.lumie.notification.application.dto.request.RecipientRequest;
import com.lumie.notification.application.dto.request.SendSmsRequest;
import com.lumie.notification.application.dto.request.SendToClassRequest;
import com.lumie.notification.application.dto.request.CreateSmsRecipientGroupRequest;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.dto.response.SmsRecipientGroupResponse;
import com.lumie.notification.application.dto.response.SmsRecipientGroupSummaryResponse;
import com.lumie.notification.application.service.SmsCommandService;
import com.lumie.notification.application.service.SmsQueryService;
import com.lumie.notification.application.service.SmsRecipientGroupService;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.idempotency.IdempotencyHeaderInterceptor;
import com.lumie.common.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/sms")
@RequiredArgsConstructor
public class SmsController {

    private static final String SEND_SMS_SCOPE = "POST /v1/sms/send";
    private static final String SEND_SMS_CLASS_SCOPE = "POST /v1/sms/send/class/{classId}";
    private static final String CREATE_RECIPIENT_GROUP_SCOPE = "POST /v1/sms/recipient-groups";

    private final SmsCommandService commandService;
    private final SmsQueryService queryService;
    private final SmsRecipientGroupService recipientGroupService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/send")
    public ResponseEntity<SmsMessageResponse> sendSms(
            @Valid @RequestBody SendSmsRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        return idempotencyService.executeOnceWithoutResponseBody(
                idempotencyKey,
                SEND_SMS_SCOPE,
                SendSmsIdempotencyBody.from(request),
                () -> ResponseEntity.status(HttpStatus.CREATED).body(commandService.sendSms(request))
        );
    }

    @GetMapping("/recipient-groups")
    public ResponseEntity<List<SmsRecipientGroupSummaryResponse>> listRecipientGroups() {
        return ResponseEntity.ok(recipientGroupService.listGroupSummaries());
    }

    @GetMapping("/recipient-groups/{id}")
    public ResponseEntity<SmsRecipientGroupResponse> getRecipientGroup(@PathVariable Long id) {
        return ResponseEntity.ok(recipientGroupService.getGroup(id));
    }

    @PostMapping("/recipient-groups")
    public ResponseEntity<SmsRecipientGroupResponse> createRecipientGroup(
            @Valid @RequestBody CreateSmsRecipientGroupRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        return idempotencyService.executeOnceWithoutResponseBody(
                idempotencyKey,
                CREATE_RECIPIENT_GROUP_SCOPE,
                request,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(recipientGroupService.createGroup(request))
        );
    }

    @GetMapping("/history")
    @AllowedSortFields({"createdAt", "sentAt", "scheduledAt", "status"})
    public ResponseEntity<PageResponse<SmsMessageResponse>> listMessages(
            @RequestParam(required = false) SmsStatus status,
            @RequestParam(required = false) RecipientType recipientType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long recipientGroupId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SmsMessageResponse> response = queryService.listMessages(
            status, recipientType, search, recipientGroupId, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<SmsMessageResponse> getMessage(@PathVariable Long id) {
        SmsMessageResponse response = queryService.getMessage(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/history/{id}/cancel")
    public ResponseEntity<SmsMessageResponse> cancelScheduled(@PathVariable Long id) {
        SmsMessageResponse response = commandService.cancelScheduled(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send/class/{classId}")
    public ResponseEntity<SmsMessageResponse> sendToClass(
            @PathVariable Long classId,
            @Valid @RequestBody SendToClassRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        return idempotencyService.executeOnceWithoutResponseBody(
                idempotencyKey,
                SEND_SMS_CLASS_SCOPE,
                java.util.Map.of("classId", classId, "request", request),
                () -> ResponseEntity.status(HttpStatus.CREATED).body(commandService.sendToClass(classId, request))
        );
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(CommonErrorCode.MISSING_IDEMPOTENCY_KEY);
        }
        if (idempotencyKey.length() < IdempotencyHeaderInterceptor.MIN_LENGTH
                || idempotencyKey.length() > IdempotencyHeaderInterceptor.MAX_LENGTH
                || !IdempotencyHeaderInterceptor.KEY_FORMAT.matcher(idempotencyKey).matches()) {
            throw new BusinessException(CommonErrorCode.INVALID_IDEMPOTENCY_KEY_FORMAT);
        }
    }

    private record SendSmsIdempotencyBody(
            RecipientType recipientType,
            List<RecipientRequest> recipients,
            String title,
            Long recipientGroupId,
            String content,
            Long templateId,
            Instant scheduledAt
    ) {
        private static SendSmsIdempotencyBody from(SendSmsRequest request) {
            if (request.recipientGroupId() != null) {
                return new SendSmsIdempotencyBody(
                        request.recipientType(),
                        null,
                        request.title(),
                        request.recipientGroupId(),
                        request.content(),
                        request.templateId(),
                        request.scheduledAt()
                );
            }
            return new SendSmsIdempotencyBody(
                    request.recipientType(),
                    request.recipients(),
                    request.title(),
                    null,
                    request.content(),
                    request.templateId(),
                    request.scheduledAt()
            );
        }
    }
}
