package com.lumie.tenant.customdomain.adapter.in.web;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.customdomain.application.dto.CustomDomainCheckResponse;
import com.lumie.tenant.customdomain.application.service.CustomDomainCheckService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tenant/me/custom-domain")
@RequiredArgsConstructor
public class CustomDomainController {

    private final CustomDomainCheckService customDomainCheckService;

    @PostMapping("/check")
    public ResponseEntity<CustomDomainCheckResponse> check(@Valid @RequestBody CheckRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return ResponseEntity.ok(customDomainCheckService.check(tenantSlug, request.domain()));
    }

    public record CheckRequest(@NotBlank @Size(max = 253) String domain) {}
}
