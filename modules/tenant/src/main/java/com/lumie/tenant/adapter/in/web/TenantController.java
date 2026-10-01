package com.lumie.tenant.adapter.in.web;

import com.lumie.tenant.application.dto.request.CompleteTenantOnboardingRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.PublicTenantResponse;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.out.TenantLogoStoragePort.LogoStream;
import com.lumie.tenant.application.service.TenantCommandService;
import com.lumie.tenant.application.service.TenantLogoService;
import com.lumie.tenant.application.service.TenantQueryService;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.domain.exception.TenantErrorCode;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantCommandService tenantCommandService;
    private final TenantQueryService tenantQueryService;
    private final TenantLogoService tenantLogoService;

    @GetMapping("/public/current-branding")
    public ResponseEntity<PublicTenantResponse> getCurrentPublicTenantBranding(
            @RequestParam(required = false) String host) {
        if (host != null && !host.isBlank()) {
            return ResponseEntity.ok(tenantQueryService.getPublicTenantForHost(host));
        }
        String tenantSlug = TenantContextHolder.getTenant();
        if (tenantSlug == null) {
            throw new ResourceNotFoundException("Tenant", "request host");
        }
        return ResponseEntity.ok(tenantQueryService.getCurrentPublicTenant(tenantSlug));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String slug) {
        requireCurrentTenant(slug);
        TenantResponse response = tenantQueryService.getTenantBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        String slug = TenantContextHolder.getTenant();
        requireCurrentTenant(slug);
        return ResponseEntity.ok(List.of(tenantQueryService.getTenantBySlug(slug)));
    }

    @PatchMapping("/{slug}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable String slug,
            @Valid @RequestBody UpdateTenantRequest request) {
        requireCurrentOwner(slug);
        TenantResponse response = tenantCommandService.updateTenant(slug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/complete-onboarding")
    public ResponseEntity<TenantResponse> completeOnboarding(
            @PathVariable String slug,
            @Valid @RequestBody CompleteTenantOnboardingRequest request) {
        TenantResponse response = tenantCommandService.completeOnboarding(slug, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteTenant(@PathVariable String slug) {
        requireCurrentOwner(slug);
        tenantCommandService.deleteTenant(slug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{slug}/suspend")
    public ResponseEntity<TenantResponse> suspendTenant(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "Manual suspension") String reason) {
        requireCurrentOwner(slug);
        TenantResponse response = tenantCommandService.suspendTenant(slug, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/reactivate")
    public ResponseEntity<TenantResponse> reactivateTenant(@PathVariable String slug) {
        requireCurrentOwner(slug);
        TenantResponse response = tenantCommandService.reactivateTenant(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{slug}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantResponse> uploadLogo(
            @PathVariable String slug,
            @RequestParam("file") MultipartFile file) {
        requireCurrentOwner(slug);
        tenantLogoService.uploadLogo(slug, file);
        return ResponseEntity.ok(tenantQueryService.getTenantBySlug(slug));
    }

    @DeleteMapping("/{slug}/logo")
    public ResponseEntity<TenantResponse> deleteLogo(@PathVariable String slug) {
        requireCurrentOwner(slug);
        tenantLogoService.deleteLogo(slug);
        return ResponseEntity.ok(tenantQueryService.getTenantBySlug(slug));
    }

    @GetMapping("/{slug}/logo")
    public ResponseEntity<InputStreamResource> getLogo(@PathVariable String slug) {
        LogoStream logo = tenantLogoService.downloadLogo(slug);
        if (logo == null) {
            throw new ResourceNotFoundException("Tenant logo", slug);
        }
        MediaType type = logo.contentType() != null
                ? MediaType.parseMediaType(logo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .body(new InputStreamResource(logo.stream()));
    }

    private static void requireCurrentOwner(String slug) {
        AuthorizationGuard.requireOwner();
        requireCurrentTenant(slug);
    }

    private static void requireCurrentTenant(String slug) {
        String currentTenant = TenantContextHolder.getTenant();
        if (currentTenant == null || !currentTenant.equals(slug)) {
            throw new BusinessException(TenantErrorCode.TENANT_CONTEXT_MISMATCH,
                    "요청 학원과 인증 학원이 일치하지 않습니다.");
        }
    }
}
