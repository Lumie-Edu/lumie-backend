package com.lumie.notification.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.notification.application.dto.request.UpsertSmsSenderSettingRequest;
import com.lumie.notification.application.dto.response.SmsSenderSettingResponse;
import com.lumie.notification.application.dto.response.SmsSenderSettingStatusResponse;
import com.lumie.notification.application.service.SmsSenderSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sms/sender-setting")
@RequiredArgsConstructor
public class SmsSenderSettingController {

    private final SmsSenderSettingService senderSettingService;

    @GetMapping
    public ResponseEntity<SmsSenderSettingResponse> getSenderSetting() {
        AuthorizationGuard.requireOwner();
        return ResponseEntity.ok(senderSettingService.getSenderSetting());
    }

    @GetMapping("/status")
    public ResponseEntity<SmsSenderSettingStatusResponse> getSenderSettingStatus() {
        return ResponseEntity.ok(senderSettingService.getSenderSettingStatus());
    }

    @PutMapping
    public ResponseEntity<SmsSenderSettingResponse> upsertSenderSetting(
            @Valid @RequestBody UpsertSmsSenderSettingRequest request) {
        AuthorizationGuard.requireOwner();
        return ResponseEntity.ok(senderSettingService.upsertSenderSetting(request));
    }
}
