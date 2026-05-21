package com.lumie.notification.adapter.in.web;

import com.lumie.notification.application.dto.request.SendSmsRequest;
import com.lumie.notification.application.dto.request.SendToClassRequest;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.service.SmsCommandService;
import com.lumie.notification.application.service.SmsQueryService;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
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


@RestController
@RequestMapping("/v1/sms")
@RequiredArgsConstructor
public class SmsController {

    private static final String SEND_SMS_SCOPE = "POST /v1/sms/send";
    private static final String SEND_SMS_CLASS_SCOPE = "POST /v1/sms/send/class/{classId}";

    private final SmsCommandService commandService;
    private final SmsQueryService queryService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/send")
    public ResponseEntity<SmsMessageResponse> sendSms(
            @Valid @RequestBody SendSmsRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    SEND_SMS_SCOPE,
                    request,
                    () -> ResponseEntity.status(HttpStatus.CREATED).body(commandService.sendSms(request))
            );
        }
        SmsMessageResponse response = commandService.sendSms(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    @AllowedSortFields({"createdAt", "sentAt", "status"})
    public ResponseEntity<PageResponse<SmsMessageResponse>> listMessages(
            @RequestParam(required = false) SmsStatus status,
            @RequestParam(required = false) RecipientType recipientType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SmsMessageResponse> response = queryService.listMessages(
            status, recipientType, search, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<SmsMessageResponse> getMessage(@PathVariable Long id) {
        SmsMessageResponse response = queryService.getMessage(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send/class/{classId}")
    public ResponseEntity<SmsMessageResponse> sendToClass(
            @PathVariable Long classId,
            @Valid @RequestBody SendToClassRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    SEND_SMS_CLASS_SCOPE,
                    java.util.Map.of("classId", classId, "request", request),
                    () -> ResponseEntity.status(HttpStatus.CREATED).body(commandService.sendToClass(classId, request))
            );
        }
        SmsMessageResponse response = commandService.sendToClass(classId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
