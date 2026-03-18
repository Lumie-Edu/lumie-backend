package com.lumie.tenant.adapter.in.web;

import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final CreateTenantUseCase createTenantUseCase;
    private final GetTenantUseCase getTenantUseCase;
    private final UpdateTenantUseCase updateTenantUseCase;
    private final DeleteTenantUseCase deleteTenantUseCase;
    private final SuspendTenantUseCase suspendTenantUseCase;

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = createTenantUseCase.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String slug) {
        TenantResponse response = getTenantUseCase.getTenantBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        List<TenantResponse> response = getTenantUseCase.getAllTenants();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{slug}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable String slug,
            @Valid @RequestBody UpdateTenantRequest request) {
        TenantResponse response = updateTenantUseCase.updateTenant(slug, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String slug) {
        deleteTenantUseCase.deleteTenant(slug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{slug}/suspend")
    public ResponseEntity<TenantResponse> suspendTenant(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "Manual suspension") String reason) {
        TenantResponse response = suspendTenantUseCase.suspendTenant(slug, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/reactivate")
    public ResponseEntity<TenantResponse> reactivateTenant(@PathVariable String slug) {
        TenantResponse response = suspendTenantUseCase.reactivateTenant(slug);
        return ResponseEntity.ok(response);
    }
}
