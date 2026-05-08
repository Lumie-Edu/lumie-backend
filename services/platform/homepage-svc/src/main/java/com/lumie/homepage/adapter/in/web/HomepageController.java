package com.lumie.homepage.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.homepage.application.dto.request.PublishHomepageRequest;
import com.lumie.homepage.application.dto.request.UpdateHomepageConfigRequest;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;
import com.lumie.homepage.application.port.in.GetHomepageConfigUseCase;
import com.lumie.homepage.application.port.in.PublishHomepageUseCase;
import com.lumie.homepage.application.port.in.UpdateHomepageConfigUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/homepage")
@RequiredArgsConstructor
public class HomepageController {

    private final GetHomepageConfigUseCase getHomepageConfigUseCase;
    private final UpdateHomepageConfigUseCase updateHomepageConfigUseCase;
    private final PublishHomepageUseCase publishHomepageUseCase;

    /**
     * GET /v1/homepage — returns the current tenant's homepage config.
     * Returns 200 with null body if no config exists yet.
     * Requires authenticated user (tenant context set by Kong via X-Tenant-Slug header).
     */
    @GetMapping
    public ResponseEntity<HomepageConfigResponse> getCurrent() {
        return ResponseEntity.ok(getHomepageConfigUseCase.getCurrent().orElse(null));
    }

    /**
     * PUT /v1/homepage — upserts the homepage config.
     * OWNER role required.
     */
    @PutMapping
    public ResponseEntity<HomepageConfigResponse> update(@Valid @RequestBody UpdateHomepageConfigRequest request) {
        AuthorizationGuard.requireOwner();
        HomepageConfigResponse response = updateHomepageConfigUseCase.update(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /v1/homepage/publish — toggles the publish flag.
     * OWNER role required.
     */
    @PostMapping("/publish")
    public ResponseEntity<HomepageConfigResponse> publish(@Valid @RequestBody PublishHomepageRequest request) {
        AuthorizationGuard.requireOwner();
        HomepageConfigResponse response = publishHomepageUseCase.publish(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /v1/homepage/public/by-custom-id/{customId} — anonymous, no tenant header.
     * Returns config only if published=true; otherwise returns 200 with null body.
     */
    @GetMapping("/public/by-custom-id/{customId}")
    public ResponseEntity<HomepageConfigResponse> getPublicByCustomId(@PathVariable String customId) {
        return ResponseEntity.ok(getHomepageConfigUseCase.getPublicByCustomId(customId).orElse(null));
    }
}
