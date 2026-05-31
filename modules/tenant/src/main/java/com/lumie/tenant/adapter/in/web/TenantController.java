package com.lumie.tenant.adapter.in.web;

import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.PublicTenantResponse;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.out.TenantLogoStoragePort.LogoStream;
import com.lumie.tenant.application.service.TenantCommandService;
import com.lumie.tenant.application.service.TenantLogoService;
import com.lumie.tenant.application.service.TenantQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Validated
@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantCommandService tenantCommandService;
    private final TenantQueryService tenantQueryService;
    private final TenantLogoService tenantLogoService;

    @GetMapping("/public/by-custom-id/{customId}")
    public ResponseEntity<PublicTenantResponse> getPublicTenantByCustomId(@PathVariable String customId) {
        return ResponseEntity.ok(tenantQueryService.getPublicTenantByCustomId(customId));
    }

    @GetMapping("/public/by-domain")
    public ResponseEntity<PublicTenantResponse> getPublicTenantByCustomDomain(
            @NotBlank @RequestParam String host) {
        String canonicalHost = host.toLowerCase(Locale.ROOT).trim();
        return ResponseEntity.ok(tenantQueryService.getPublicTenantByCustomDomain(canonicalHost));
    }

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = tenantCommandService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String slug) {
        TenantResponse response = tenantQueryService.getTenantBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        List<TenantResponse> response = tenantQueryService.getAllTenants();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{slug}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable String slug,
            @Valid @RequestBody UpdateTenantRequest request) {
        TenantResponse response = tenantCommandService.updateTenant(slug, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String slug) {
        tenantCommandService.deleteTenant(slug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{slug}/suspend")
    public ResponseEntity<TenantResponse> suspendTenant(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "Manual suspension") String reason) {
        TenantResponse response = tenantCommandService.suspendTenant(slug, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/reactivate")
    public ResponseEntity<TenantResponse> reactivateTenant(@PathVariable String slug) {
        TenantResponse response = tenantCommandService.reactivateTenant(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{slug}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantResponse> uploadLogo(
            @PathVariable String slug,
            @RequestParam("file") MultipartFile file) {
        tenantLogoService.uploadLogo(slug, file);
        return ResponseEntity.ok(tenantQueryService.getTenantBySlug(slug));
    }

    @DeleteMapping("/{slug}/logo")
    public ResponseEntity<TenantResponse> deleteLogo(@PathVariable String slug) {
        tenantLogoService.deleteLogo(slug);
        return ResponseEntity.ok(tenantQueryService.getTenantBySlug(slug));
    }

    @GetMapping("/{slug}/logo")
    public ResponseEntity<InputStreamResource> getLogo(@PathVariable String slug) {
        LogoStream logo = tenantLogoService.downloadLogo(slug);
        if (logo == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = logo.contentType() != null
                ? MediaType.parseMediaType(logo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(new InputStreamResource(logo.stream()));
    }
}
