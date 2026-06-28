package com.lumie.notification.adapter.in.web;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.notification.application.dto.request.UpdateSmsSenderSettingApprovalRequest;
import com.lumie.notification.application.dto.response.SmsSenderSettingResponse;
import com.lumie.notification.application.service.SmsSenderSettingService;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.tenant.api.TenantService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/sms/sender-settings")
@RequiredArgsConstructor
@Hidden
public class InternalSmsSenderSettingController {

    private final SmsSenderSettingService senderSettingService;
    private final TenantService tenantService;

    @PatchMapping("/{tenantSlug}/approval")
    public ResponseEntity<SmsSenderSettingResponse> updateApproval(
            @PathVariable String tenantSlug,
            @Valid @RequestBody UpdateSmsSenderSettingApprovalRequest request) {
        String authenticatedTenantSlug = TenantContextHolder.getTenant();
        if (authenticatedTenantSlug != null && !tenantSlug.equals(authenticatedTenantSlug)) {
            throw new BusinessException(
                    CommonErrorCode.FORBIDDEN,
                    "Internal sender-setting approval tenant does not match authenticated tenant"
            );
        }
        TenantService.TenantData tenant = tenantService.getTenantBySlug(tenantSlug)
                .orElseThrow(() -> new BusinessException(
                        SmsErrorCode.SENDER_SETTING_NOT_FOUND,
                        "Tenant not found for sender-setting approval: " + tenantSlug
                ));
        SmsSenderSettingResponse response = TenantContextHolder.withinContext(
                tenant.slug(),
                tenant.id(),
                () -> senderSettingService.updateApproval(request)
        );
        return ResponseEntity.ok(response);
    }
}
