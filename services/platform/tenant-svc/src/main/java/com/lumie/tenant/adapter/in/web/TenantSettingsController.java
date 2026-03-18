package com.lumie.tenant.adapter.in.web;

import com.lumie.tenant.application.dto.request.TenantSettingsRequest;
import com.lumie.tenant.application.dto.response.TenantSettingsResponse;
import com.lumie.tenant.application.port.in.GetTenantSettingsUseCase;
import com.lumie.tenant.application.port.in.UpdateTenantSettingsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tenants/{slug}/settings")
@RequiredArgsConstructor
public class TenantSettingsController {

    private final GetTenantSettingsUseCase getTenantSettingsUseCase;
    private final UpdateTenantSettingsUseCase updateTenantSettingsUseCase;

    @GetMapping
    public ResponseEntity<TenantSettingsResponse> getSettings(@PathVariable String slug) {
        TenantSettingsResponse response = getTenantSettingsUseCase.getSettings(slug);
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<TenantSettingsResponse> updateSettings(
            @PathVariable String slug,
            @Valid @RequestBody TenantSettingsRequest request) {
        TenantSettingsResponse response = updateTenantSettingsUseCase.updateSettings(slug, request);
        return ResponseEntity.ok(response);
    }
}
